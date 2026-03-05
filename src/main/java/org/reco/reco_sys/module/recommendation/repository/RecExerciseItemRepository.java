package org.reco.reco_sys.module.recommendation.repository;

import org.reco.reco_sys.module.recommendation.entity.RecExerciseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecExerciseItemRepository extends JpaRepository<RecExerciseItem, Long> {
    List<RecExerciseItem> findByRecIdOrderByRankOrder(Long recId);
}
