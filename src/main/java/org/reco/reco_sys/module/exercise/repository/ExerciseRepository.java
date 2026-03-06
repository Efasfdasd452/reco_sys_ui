package org.reco.reco_sys.module.exercise.repository;

import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Page<Exercise> findByCourseId(Long courseId, Pageable pageable);
    long countByCourseId(Long courseId);
    java.util.Optional<Exercise> findByPyExIndex(Integer pyExIndex);
    boolean existsByPyExIndexIsNotNull();
}
