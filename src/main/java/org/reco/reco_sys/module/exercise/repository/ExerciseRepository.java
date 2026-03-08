package org.reco.reco_sys.module.exercise.repository;

import org.reco.reco_sys.module.exercise.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Page<Exercise> findByCourseId(Long courseId, Pageable pageable);

    /** 支持关键字搜索（content 模糊匹配），keyword 为空时返回全部 */
    @Query("SELECT e FROM Exercise e WHERE e.courseId = :courseId " +
           "AND (:keyword = '' OR LOWER(e.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Exercise> searchByCourseId(@Param("courseId") Long courseId,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);
    java.util.List<Exercise> findByCourseId(Long courseId);
    long countByCourseId(Long courseId);
    java.util.Optional<Exercise> findByPyExIndex(Integer pyExIndex);
    boolean existsByPyExIndexIsNotNull();
    boolean existsByPyExIndexAndCourseId(Integer pyExIndex, Long courseId);
    java.util.Optional<Exercise> findByPyExIndexAndCourseId(Integer pyExIndex, Long courseId);
}
