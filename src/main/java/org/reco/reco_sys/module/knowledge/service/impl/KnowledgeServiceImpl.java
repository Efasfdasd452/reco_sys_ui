package org.reco.reco_sys.module.knowledge.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.entity.ExerciseKpRel;
import org.reco.reco_sys.module.exercise.repository.ExerciseKpRelRepository;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.knowledge.dto.GraphDto;
import org.reco.reco_sys.module.knowledge.dto.KnowledgePointDto;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNeo4jRepository;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNode;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.knowledge.service.KnowledgeService;
import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.reco.reco_sys.module.learning.entity.UserKcState;
import org.reco.reco_sys.module.learning.repository.AnswerRecordRepository;
import org.reco.reco_sys.module.learning.repository.UserKcStateRepository;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgePointRepository kpRepository;
    private final KnowledgePointNeo4jRepository kpNeo4jRepository;
    private final UserKcStateRepository kcStateRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseKpRelRepository exerciseKpRelRepository;
    private final Neo4jClient neo4jClient;

    /** 最大遗忘周期（分钟），超过此时长 exfr = 1.0 */
    private static final double MAX_FORGET_MINUTES = 7 * 24 * 60.0; // 7天

    @Override
    public List<KnowledgePointDto> listByCourse(Long courseId) {
        List<KnowledgePoint> all = kpRepository.findByCourseId(courseId);
        Map<Long, KnowledgePointDto> map = all.stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, this::toDto));
        List<KnowledgePointDto> roots = new ArrayList<>();
        for (KnowledgePointDto dto : map.values()) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                KnowledgePointDto parent = map.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(dto);
                }
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public KnowledgePointDto create(KnowledgePointDto dto, Long teacherId) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setCourseId(dto.getCourseId());
        kp.setName(dto.getName());
        kp.setDescription(dto.getDescription());
        kp.setParentId(dto.getParentId());
        KnowledgePoint saved = kpRepository.save(kp);

        // Sync to Neo4j
        KnowledgePointNode node = new KnowledgePointNode();
        node.setMysqlId(saved.getId());
        node.setName(saved.getName());
        node.setCourseId(String.valueOf(saved.getCourseId()));
        kpNeo4jRepository.save(node);

        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        kpRepository.deleteById(id);
        kpNeo4jRepository.findByMysqlId(id).ifPresent(kpNeo4jRepository::delete);
    }

    @Override
    @Transactional
    public void addRelation(Long fromId, Long toId, String relationType) {
        // 关系现在通过 Neo4jClient 维护，此接口保留用于兼容，不做操作
        log.info("addRelation called: {} -> {} type={}", fromId, toId, relationType);
    }

    @Override
    public GraphDto getGraphForStudent(Long courseId, Long userId, boolean includePrerequisites, int maxRelatedNodes) {
        return buildGraph(courseId, userId, includePrerequisites, maxRelatedNodes);
    }

    @Override
    public GraphDto getGraphForTeacher(Long courseId, Long targetUserId, boolean includePrerequisites, int maxRelatedNodes) {
        return buildGraph(courseId, targetUserId, includePrerequisites, maxRelatedNodes);
    }

    /**
     * 按文档《新学生个人知识图谱设计》构建以学生为中心的图谱：
     *
     * 节点：student（虚拟中心）+ kc（已交互的知识点）+ exercise（已答题目）
     * 边：
     *   kc  ──[mlkc=X%]──→ student   (掌握度)
     *   kc  ──[pkc=X%]──→  student   (出现概率 = totalCount/总答题数)
     *   ex  ──[exfr=X]──→  student   (遗忘率)
     *   ex  ──[covers]──→  kc        (Q矩阵静态覆盖关系)
     *
     * 全部从 MySQL 计算，无需查 Neo4j（Neo4j 供 Python 推荐模型使用）。
     */
    private GraphDto buildGraph(Long courseId, Long userId, boolean includePrerequisites, int maxRelatedNodes) {
        List<GraphDto.GraphNode> nodes = new ArrayList<>();
        List<GraphDto.GraphEdge> edges = new ArrayList<>();

        // ── 0. 学生中心节点 ─────────────────────────────────────────────
        GraphDto.GraphNode studentNode = new GraphDto.GraphNode();
        studentNode.setId("student");
        studentNode.setNodeType("student");
        studentNode.setLabel("我");
        nodes.add(studentNode);

        // ── 1. KC 节点 ──────────────────────────────────────────────────
        List<UserKcState> states = kcStateRepository.findByUserId(userId);
        List<KnowledgePoint> allCourseKps = kpRepository.findByCourseId(courseId);
        Map<Long, KnowledgePoint> kpById = allCourseKps.stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, kp -> kp));

        // pkc 分母：该学生总答题数
        long totalExercisesDone = answerRecordRepository.countDistinctExerciseIdsByUserId(userId);

        Set<Long> visibleKpIds = new LinkedHashSet<>();
        for (UserKcState state : states) {
            KnowledgePoint kp = kpById.get(state.getKpId());
            if (kp == null) continue; // 不属于本课程
            visibleKpIds.add(kp.getId());

            double mastery = state.getMasteryLevel();
            double pkc = totalExercisesDone > 0
                    ? Math.min(1.0, (double) state.getTotalCount() / totalExercisesDone)
                    : 0.0;

            GraphDto.GraphNode node = new GraphDto.GraphNode();
            node.setId("kc_" + kp.getId());
            node.setNodeType("kc");
            node.setLabel(kp.getName());
            node.setMasteryLevel(mastery);
            node.setPkc(pkc);
            nodes.add(node);

            // kc → student: mlkc 边
            GraphDto.GraphEdge mlkcEdge = new GraphDto.GraphEdge();
            mlkcEdge.setSource("kc_" + kp.getId());
            mlkcEdge.setTarget("student");
            mlkcEdge.setLabel(String.format("mlkc=%.0f%%", mastery * 100));
            mlkcEdge.setEdgeType("mlkc");
            edges.add(mlkcEdge);

            // kc → student: pkc 边
            GraphDto.GraphEdge pkcEdge = new GraphDto.GraphEdge();
            pkcEdge.setSource("kc_" + kp.getId());
            pkcEdge.setTarget("student");
            pkcEdge.setLabel(String.format("pkc=%.0f%%", pkc * 100));
            pkcEdge.setEdgeType("pkc");
            edges.add(pkcEdge);
        }

        // 冷启动：尚未答题，展示前 20 个 KC 供参考（无连线到 student）
        if (visibleKpIds.isEmpty()) {
            allCourseKps.stream().limit(20).forEach(kp -> {
                visibleKpIds.add(kp.getId());
                GraphDto.GraphNode node = new GraphDto.GraphNode();
                node.setId("kc_" + kp.getId());
                node.setNodeType("kc");
                node.setLabel(kp.getName());
                node.setMasteryLevel(0.0);
                node.setPkc(0.0);
                nodes.add(node);
            });
        }

        // ── 2. Exercise 节点 + exfr 边 ───────────────────────────────────
        List<Long> answeredExIds = answerRecordRepository.findDistinctExerciseIdsByUserId(userId);
        List<Exercise> answeredExercises = answeredExIds.isEmpty()
                ? Collections.emptyList()
                : exerciseRepository.findAllById(answeredExIds).stream()
                        .filter(e -> courseId.equals(e.getCourseId()))
                        .collect(Collectors.toList());

        Map<Long, Double> exfrMap = new HashMap<>();
        if (!answeredExIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<AnswerRecord> latestRecords =
                    answerRecordRepository.findLatestPerExercise(userId, answeredExIds);
            for (AnswerRecord ar : latestRecords) {
                long minutes = ChronoUnit.MINUTES.between(ar.getSubmittedAt(), now);
                exfrMap.put(ar.getExerciseId(), Math.min(minutes / MAX_FORGET_MINUTES, 1.0));
            }
        }

        Set<Long> visibleExIds = new LinkedHashSet<>();
        for (Exercise ex : answeredExercises) {
            visibleExIds.add(ex.getId());
            double exfr = exfrMap.getOrDefault(ex.getId(), 0.0);
            String exLabel = ex.getPyExIndex() != null ? "ex" + ex.getPyExIndex() : "ex#" + ex.getId();

            GraphDto.GraphNode node = new GraphDto.GraphNode();
            node.setId("ex_" + ex.getId());
            node.setNodeType("exercise");
            node.setLabel(exLabel);
            node.setExfr(exfr);
            nodes.add(node);

            // ex → student: exfr 边
            GraphDto.GraphEdge exfrEdge = new GraphDto.GraphEdge();
            exfrEdge.setSource("ex_" + ex.getId());
            exfrEdge.setTarget("student");
            exfrEdge.setLabel(String.format("exfr=%.2f", exfr));
            exfrEdge.setEdgeType("exfr");
            edges.add(exfrEdge);
        }

        // ── 3. 前置依赖扩展（可选）：从 Neo4j 递归查 PREREQUISITE_OF ───
        if (includePrerequisites && !visibleKpIds.isEmpty()) {
            int cap = maxRelatedNodes <= 0 ? Integer.MAX_VALUE : maxRelatedNodes;
            expandPrerequisites(nodes, edges, visibleKpIds, kpById, cap);
        }

        // ── 4. COVERS 边：exercise → kc（Q矩阵静态，仅可见节点间）─────
        for (Long exId : visibleExIds) {
            for (ExerciseKpRel rel : exerciseKpRelRepository.findByExerciseId(exId)) {
                if (visibleKpIds.contains(rel.getKpId())) {
                    GraphDto.GraphEdge edge = new GraphDto.GraphEdge();
                    edge.setSource("ex_" + exId);
                    edge.setTarget("kc_" + rel.getKpId());
                    edge.setLabel("covers");
                    edge.setEdgeType("covers");
                    edges.add(edge);
                }
            }
        }

        GraphDto graph = new GraphDto();
        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
    }

    /**
     * 从 Neo4j 以 BFS 方式展开 visibleKpIds 的 RELATED_TO 邻居（最多 2 跳），
     * 新增节点数不超过 cap（0 或 Integer.MAX_VALUE 表示不限）。
     * 新增节点 nodeType="prereq"，边 edgeType="prereq"。
     */
    private void expandPrerequisites(List<GraphDto.GraphNode> nodes,
                                     List<GraphDto.GraphEdge> edges,
                                     Set<Long> visibleKpIds,
                                     Map<Long, KnowledgePoint> kpById,
                                     int cap) {
        // BFS: 第 1 跳从 seed 出发，第 2 跳从第 1 跳新节点出发
        Set<Long> visited = new HashSet<>(visibleKpIds);   // 已在图中的节点
        Set<Long> newIds  = new LinkedHashSet<>();          // 本次新引入的节点
        Set<String> edgeKeys = new HashSet<>();

        // 边缓存（source mysqlId, target mysqlId）
        List<long[]> pendingEdges = new ArrayList<>();

        // 执行 1 跳 BFS，返回从 currentFrontier 出发找到的 (seedId, relId) 对
        java.util.function.BiConsumer<List<Long>, Integer> bfsStep = (frontier, depth) -> {
            if (frontier.isEmpty()) return;
            Collection<Map<String, Object>> rows;
            try {
                rows = neo4jClient.query(
                        "UNWIND $frontier AS sid " +
                        "MATCH (seed:KnowledgePoint {mysqlId: sid})-[:RELATED_TO]-(rel:KnowledgePoint) " +
                        "WHERE NOT rel.mysqlId IN $visited " +
                        "RETURN seed.mysqlId AS seedId, rel.mysqlId AS relId"
                ).bind(frontier).to("frontier")
                 .bind(new ArrayList<>(visited)).to("visited")
                 .fetch().all();
            } catch (Exception e) {
                log.warn("Neo4j RELATED_TO 查询失败(depth={}): {}", depth, e.getMessage());
                return;
            }
            for (Map<String, Object> row : rows) {
                long seedId = ((Number) row.get("seedId")).longValue();
                long relId  = ((Number) row.get("relId")).longValue();
                if (newIds.size() < cap && !visited.contains(relId)) {
                    newIds.add(relId);
                    visited.add(relId);
                }
                pendingEdges.add(new long[]{seedId, relId});
            }
        };

        // 第 1 跳：seed KCs
        bfsStep.accept(new ArrayList<>(visibleKpIds), 1);

        // 第 2 跳：第 1 跳新引入的节点
        if (!newIds.isEmpty() && newIds.size() < cap) {
            bfsStep.accept(new ArrayList<>(newIds), 2);
        }

        if (newIds.isEmpty()) return;

        // 从 MySQL 查新节点详情并加节点
        List<KnowledgePoint> relKps = kpRepository.findAllById(newIds);
        for (KnowledgePoint kp : relKps) {
            GraphDto.GraphNode node = new GraphDto.GraphNode();
            node.setId("kc_" + kp.getId());
            node.setNodeType("prereq");
            node.setLabel(kp.getName());
            node.setMasteryLevel(0.0);
            node.setPkc(0.0);
            nodes.add(node);
        }

        // 加边（仅两端都在图中的边）
        Set<Long> allIds = new HashSet<>(visibleKpIds);
        allIds.addAll(newIds);
        for (long[] e : pendingEdges) {
            long a = e[0], b = e[1];
            if (!allIds.contains(a) || !allIds.contains(b)) continue;
            String key = Math.min(a, b) + "-" + Math.max(a, b);
            if (!edgeKeys.add(key)) continue;
            GraphDto.GraphEdge edge = new GraphDto.GraphEdge();
            edge.setSource("kc_" + a);
            edge.setTarget("kc_" + b);
            edge.setLabel("关联");
            edge.setEdgeType("prereq");
            edges.add(edge);
        }
    }

    private KnowledgePointDto toDto(KnowledgePoint kp) {
        KnowledgePointDto dto = new KnowledgePointDto();
        dto.setId(kp.getId());
        dto.setCourseId(kp.getCourseId());
        dto.setName(kp.getName());
        dto.setDescription(kp.getDescription());
        dto.setParentId(kp.getParentId());
        return dto;
    }
}
