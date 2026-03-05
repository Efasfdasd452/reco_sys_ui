package org.reco.reco_sys.module.exercise.repository;

import org.reco.reco_sys.module.exercise.entity.ExerciseKpRel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseKpRelRepository extends JpaRepository<ExerciseKpRel, Long> {
    List<ExerciseKpRel> findByExerciseId(Long exerciseId);
    List<ExerciseKpRel> findByKpId(Long kpId);
    void deleteByExerciseId(Long exerciseId);
}
