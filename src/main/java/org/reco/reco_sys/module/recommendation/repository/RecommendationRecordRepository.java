package org.reco.reco_sys.module.recommendation.repository;

import org.reco.reco_sys.module.recommendation.entity.RecommendationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRecordRepository extends JpaRepository<RecommendationRecord, Long> {
    Optional<RecommendationRecord> findTopByUserIdAndCourseIdOrderByCreatedAtDesc(Long userId, Long courseId);
    List<RecommendationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
