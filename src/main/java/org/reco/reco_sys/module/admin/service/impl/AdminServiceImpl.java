package org.reco.reco_sys.module.admin.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.admin.service.AdminService;
import org.reco.reco_sys.module.course.entity.Course;
import org.reco.reco_sys.module.course.repository.CourseRepository;
import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.reco.reco_sys.module.exercise.entity.ExerciseKpRel;
import org.reco.reco_sys.module.exercise.repository.ExerciseKpRelRepository;
import org.reco.reco_sys.module.exercise.repository.ExerciseRepository;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNeo4jRepository;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNode;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.recommendation.client.PythonRecommendClient;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.reco.reco_sys.module.user.service.impl.UserServiceImpl;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final SysUserRepository userRepository;
    private final UserServiceImpl userServiceImpl;
    private final CourseRepository courseRepository;
    private final KnowledgePointRepository kpRepository;
    private final KnowledgePointNeo4jRepository kpNeo4jRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseKpRelRepository kpRelRepository;
    private final PythonRecommendClient pythonClient;
    private final Neo4jClient neo4jClient;

    @Override
    public List<UserProfileDto> listUsers() {
        return userRepository.findAll().stream()
                .map(userServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setUserRole(Long userId, String role) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        user.setRole(SysUser.Role.valueOf(role));
        userRepository.save(user);
    }

    /**
     * 从 Python 推荐服务导入 KG4Ex 数据（112个知识点 + 1084道习题）。
     * 幂等操作：若检测到已有 pyExIndex 数据则直接返回。
     */
    @Override
    @Transactional
    public Map<String, Object> initPythonData(Long adminUserId) {
        if (exerciseRepository.existsByPyExIndexIsNotNull()) {
            log.info("Python数据已初始化，跳过");
            return Map.of("status", "already_initialized",
                    "message", "数据已经初始化过，无需重复操作");
        }

        log.info("开始从Python推荐服务导入KG4Ex数据...");

        // 1. 创建默认课程
        Course defaultCourse = new Course();
        defaultCourse.setName("Algebra 2005 数学练习");
        defaultCourse.setDescription("KG4Ex推荐模型（CIKM'23）基于Algebra 2005数据集训练，本课程包含该数据集全部1084道习题与112个知识点。");
        defaultCourse.setTeacherId(adminUserId);
        defaultCourse = courseRepository.save(defaultCourse);
        log.info("创建默认课程 id={}", defaultCourse.getId());

        // 2. 导入112个知识点
        List<PythonRecommendClient.KcItem> kcItems = pythonClient.listKnowledgeConcepts();
        Map<Integer, Long> kcIndexToMysqlId = new HashMap<>();

        for (PythonRecommendClient.KcItem kc : kcItems) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setCourseId(defaultCourse.getId());
            kp.setName(kc.kcName());
            kp.setDescription("Algebra 2005 知识点（" + kc.kcName() + "）");
            kp.setPyKcIndex(kc.kcId());
            KnowledgePoint saved = kpRepository.save(kp);
            kcIndexToMysqlId.put(kc.kcId(), saved.getId());

            // Sync to Neo4j
            KnowledgePointNode node = new KnowledgePointNode();
            node.setMysqlId(saved.getId());
            node.setName(kc.kcName());
            node.setCourseId(String.valueOf(defaultCourse.getId()));
            kpNeo4jRepository.save(node);
        }
        log.info("导入知识点 {} 个", kcItems.size());

        // 3. 导入1084道习题（含KP关联）
        List<PythonRecommendClient.ExItem> exItems = pythonClient.listExercises();
        int exCount = 0;
        for (PythonRecommendClient.ExItem ex : exItems) {
            Exercise exercise = new Exercise();
            exercise.setCourseId(defaultCourse.getId());
            exercise.setType(Exercise.Type.SHORT_ANSWER);
            exercise.setDifficulty(Exercise.Difficulty.MEDIUM);
            exercise.setContent("**[" + ex.exName() + "]** Algebra 2005 数学习题");
            exercise.setCreatorId(adminUserId);
            exercise.setPyExIndex(ex.exId());
            Exercise savedEx = exerciseRepository.save(exercise);

            for (Integer kcIdx : ex.kcIds()) {
                Long kpMysqlId = kcIndexToMysqlId.get(kcIdx);
                if (kpMysqlId != null) {
                    ExerciseKpRel rel = new ExerciseKpRel();
                    rel.setExerciseId(savedEx.getId());
                    rel.setKpId(kpMysqlId);
                    kpRelRepository.save(rel);
                }
            }
            exCount++;
        }
        log.info("导入习题 {} 道", exCount);

        Map<String, Object> result = new HashMap<>(Map.of(
                "status", "success",
                "courseId", defaultCourse.getId(),
                "kcs", kcItems.size(),
                "exercises", exCount
        ));
        // 顺带建 RELATED_TO 关系（利用刚导入的 ExerciseKpRel 数据）
        result.putAll(syncNeo4jRelations(defaultCourse.getId()));
        return result;
    }

    /**
     * 按文档《新学生个人知识图谱设计》同步 Neo4j：
     * - KnowledgePoint 节点（KC）
     * - Exercise 节点
     * - Exercise -[:COVERS]-> KnowledgePoint 边（来自 Q 矩阵 / ExerciseKpRel）
     * 不再使用 KC 间 RELATED_TO（那是旧设计遗留）。
     */
    @Override
    public Map<String, Object> syncNeo4jRelations(Long courseId) {
        // 1. MERGE KC 节点
        List<KnowledgePoint> kps = kpRepository.findByCourseId(courseId);
        List<Map<String, Object>> kcParams = kps.stream().map(kp -> {
            Map<String, Object> m = new HashMap<>();
            m.put("mysqlId", kp.getId());
            m.put("name", kp.getName());
            m.put("courseId", String.valueOf(courseId));
            return m;
        }).collect(Collectors.toList());

        neo4jClient.query(
                "UNWIND $nodes AS node " +
                "MERGE (n:KnowledgePoint {mysqlId: node.mysqlId}) " +
                "SET n.name = node.name, n.courseId = node.courseId")
                .bindAll(Map.of("nodes", kcParams))
                .run();
        log.info("Neo4j KC 节点 MERGE 完成，共 {} 个", kps.size());

        // 2. MERGE Exercise 节点
        List<Exercise> exercises = exerciseRepository.findByCourseId(courseId);
        List<Map<String, Object>> exParams = exercises.stream().map(ex -> {
            Map<String, Object> m = new HashMap<>();
            m.put("mysqlId", ex.getId());
            m.put("pyExIndex", ex.getPyExIndex() != null ? ex.getPyExIndex() : -1);
            m.put("courseId", String.valueOf(courseId));
            return m;
        }).collect(Collectors.toList());

        if (!exParams.isEmpty()) {
            neo4jClient.query(
                    "UNWIND $nodes AS node " +
                    "MERGE (e:Exercise {mysqlId: node.mysqlId}) " +
                    "SET e.pyExIndex = node.pyExIndex, e.courseId = node.courseId")
                    .bindAll(Map.of("nodes", exParams))
                    .run();
        }
        log.info("Neo4j Exercise 节点 MERGE 完成，共 {} 个", exercises.size());

        // 3. MERGE COVERS 边：Exercise -[:COVERS]-> KnowledgePoint
        List<Map<String, Object>> coversParams = new ArrayList<>();
        for (Exercise ex : exercises) {
            List<ExerciseKpRel> rels = kpRelRepository.findByExerciseId(ex.getId());
            for (ExerciseKpRel rel : rels) {
                Map<String, Object> e = new HashMap<>();
                e.put("exMysqlId", ex.getId());
                e.put("kcMysqlId", rel.getKpId());
                coversParams.add(e);
            }
        }

        if (!coversParams.isEmpty()) {
            neo4jClient.query(
                    "UNWIND $edges AS edge " +
                    "MATCH (ex:Exercise {mysqlId: edge.exMysqlId}) " +
                    "MATCH (kc:KnowledgePoint {mysqlId: edge.kcMysqlId}) " +
                    "MERGE (ex)-[:COVERS]->(kc)")
                    .bindAll(Map.of("edges", coversParams))
                    .run();
        }
        log.info("Neo4j COVERS 边 MERGE 完成，共 {} 条", coversParams.size());

        // 4. 从 Q 矩阵共现推导 PREREQUISITE_OF 边
        //    规则：若 kc_i 出现在 kc_j 所有相关习题中（P(kc_i|kc_j) >= 0.85），
        //    且 kc_i 涉及的习题数 > kc_j（说明 kc_i 更基础），则 kc_i PREREQUISITE_OF kc_j。
        Map<Long, Set<Long>> kcToExSet = new HashMap<>();
        for (Exercise ex : exercises) {
            for (ExerciseKpRel rel : kpRelRepository.findByExerciseId(ex.getId())) {
                kcToExSet.computeIfAbsent(rel.getKpId(), k -> new HashSet<>()).add(ex.getId());
            }
        }

        List<Map<String, Object>> prereqParams = new ArrayList<>();
        List<Long> kcIdList = new ArrayList<>(kcToExSet.keySet());
        for (int i = 0; i < kcIdList.size(); i++) {
            Long kcJ = kcIdList.get(i);
            Set<Long> exsJ = kcToExSet.get(kcJ);
            if (exsJ.isEmpty()) continue;
            for (int j = 0; j < kcIdList.size(); j++) {
                if (i == j) continue;
                Long kcI = kcIdList.get(j);
                Set<Long> exsI = kcToExSet.get(kcI);
                if (exsI == null || exsI.isEmpty()) continue;
                // kc_i 比 kc_j 更基础（出现习题数更多），且 kc_j 的所有习题中 kc_i 几乎都出现
                if (exsI.size() <= exsJ.size()) continue;
                long intersection = exsI.stream().filter(exsJ::contains).count();
                double conditionalProb = (double) intersection / exsJ.size();
                if (conditionalProb >= 0.85) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("fromId", kcI);
                    p.put("toId", kcJ);
                    prereqParams.add(p);
                }
            }
        }

        if (!prereqParams.isEmpty()) {
            neo4jClient.query(
                    "UNWIND $edges AS edge " +
                    "MATCH (from:KnowledgePoint {mysqlId: edge.fromId}) " +
                    "MATCH (to:KnowledgePoint {mysqlId: edge.toId}) " +
                    "MERGE (from)-[:PREREQUISITE_OF]->(to)")
                    .bindAll(Map.of("edges", prereqParams))
                    .run();
        }
        log.info("Neo4j PREREQUISITE_OF 边推导完成，共 {} 条", prereqParams.size());

        return Map.of(
                "neo4jKcNodes", kps.size(),
                "neo4jExNodes", exercises.size(),
                "neo4jCoversEdges", coversParams.size(),
                "neo4jPrereqEdges", prereqParams.size()
        );
    }

    @Override
    public void cleanOldNeo4jEdges() {
        neo4jClient.query("MATCH ()-[r:RELATED_TO]->() DELETE r").run();
        log.info("已清除 Neo4j 中旧设计的 RELATED_TO 边（PREREQUISITE_OF 由 syncNeo4j 维护，不清除）");
    }
}
