package org.reco.reco_sys.module.knowledge.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.knowledge.dto.GraphDto;
import org.reco.reco_sys.module.knowledge.dto.KnowledgePointDto;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNeo4jRepository;
import org.reco.reco_sys.module.knowledge.neo4j.KnowledgePointNode;
import org.reco.reco_sys.module.knowledge.repository.KnowledgePointRepository;
import org.reco.reco_sys.module.knowledge.service.KnowledgeService;
import org.reco.reco_sys.module.learning.entity.UserKcState;
import org.reco.reco_sys.module.learning.repository.UserKcStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgePointRepository kpRepository;
    private final KnowledgePointNeo4jRepository kpNeo4jRepository;
    private final UserKcStateRepository kcStateRepository;

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
        KnowledgePointNode from = kpNeo4jRepository.findByMysqlId(fromId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "知识点不存在"));
        KnowledgePointNode to = kpNeo4jRepository.findByMysqlId(toId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "知识点不存在"));
        if ("PREREQUISITE_OF".equals(relationType)) {
            if (from.getPrerequisites() == null) from.setPrerequisites(new ArrayList<>());
            from.getPrerequisites().add(to);
        } else {
            if (from.getRelatedPoints() == null) from.setRelatedPoints(new ArrayList<>());
            from.getRelatedPoints().add(to);
        }
        kpNeo4jRepository.save(from);
    }

    @Override
    public GraphDto getGraphForStudent(Long courseId, Long userId) {
        return buildGraph(courseId, userId);
    }

    @Override
    public GraphDto getGraphForTeacher(Long courseId, Long targetUserId) {
        return buildGraph(courseId, targetUserId);
    }

    private GraphDto buildGraph(Long courseId, Long userId) {
        List<KnowledgePoint> kps = kpRepository.findByCourseId(courseId);
        List<UserKcState> states = kcStateRepository.findByUserId(userId);
        Map<Long, Double> masteryMap = states.stream()
                .collect(Collectors.toMap(UserKcState::getKpId, UserKcState::getMasteryLevel));

        GraphDto graph = new GraphDto();
        List<GraphDto.GraphNode> nodes = kps.stream().map(kp -> {
            GraphDto.GraphNode node = new GraphDto.GraphNode();
            node.setId("kp_" + kp.getId());
            node.setLabel(kp.getName());
            node.setType("knowledge_point");
            node.setMasteryLevel(masteryMap.getOrDefault(kp.getId(), 0.0));
            return node;
        }).collect(Collectors.toList());

        List<GraphDto.GraphEdge> edges = new ArrayList<>();
        for (KnowledgePoint kp : kps) {
            if (kp.getParentId() != null) {
                GraphDto.GraphEdge edge = new GraphDto.GraphEdge();
                edge.setSource("kp_" + kp.getParentId());
                edge.setTarget("kp_" + kp.getId());
                edge.setLabel("包含");
                edges.add(edge);
            }
        }
        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
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
