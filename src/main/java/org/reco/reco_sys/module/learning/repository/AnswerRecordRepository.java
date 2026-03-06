package org.reco.reco_sys.module.learning.repository;

import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    Page<AnswerRecord> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);
    List<AnswerRecord> findByStatusIn(List<AnswerRecord.Status> statuses);

    @Query(value = "SELECT ar.* FROM answer_record ar " +
           "JOIN exercise e ON ar.exercise_id = e.id " +
           "WHERE e.course_id = :courseId AND ar.status = 'SUBMITTED' ORDER BY ar.submitted_at",
           nativeQuery = true)
    List<AnswerRecord> findPendingGradingByCourse(Long courseId);

    boolean existsByUserIdAndExerciseId(Long userId, Long exerciseId);

    @Query("SELECT DISTINCT ar.exerciseId FROM AnswerRecord ar WHERE ar.userId = :userId")
    List<Long> findDistinctExerciseIdsByUserId(Long userId);
}
