package org.reco.reco_sys.module.knowledge.repository;

import org.reco.reco_sys.module.knowledge.entity.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, Long> {
    List<KnowledgePoint> findByCourseId(Long courseId);
    List<KnowledgePoint> findByCourseIdAndParentIdIsNull(Long courseId);
}
