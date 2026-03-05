package org.reco.reco_sys.module.knowledge.service;

import org.reco.reco_sys.module.knowledge.dto.GraphDto;
import org.reco.reco_sys.module.knowledge.dto.KnowledgePointDto;
import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;

import java.util.List;

public interface KnowledgeService {
    List<KnowledgePointDto> listByCourse(Long courseId);
    KnowledgePointDto create(KnowledgePointDto dto, Long teacherId);
    void delete(Long id);
    void addRelation(Long fromId, Long toId, String relationType);
    GraphDto getGraphForStudent(Long courseId, Long userId);
    GraphDto getGraphForTeacher(Long courseId, Long targetUserId);
}
