package org.reco.reco_sys.module.learning.repository;

import org.reco.reco_sys.module.learning.entity.AnswerRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    @Modifying
    void deleteByUserId(Long userId);
    List<AnswerRecord> findByUserId(Long userId);
    Page<AnswerRecord> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);
    List<AnswerRecord> findByStatusIn(List<AnswerRecord.Status> statuses);

    @Query(value = "SELECT ar.* FROM answer_record ar " +
           "JOIN exercise e ON ar.exercise_id = e.id " +
           "WHERE e.course_id = :courseId AND ar.status IN ('SUBMITTED','GRADING') ORDER BY ar.submitted_at",
           nativeQuery = true)
    List<AnswerRecord> findPendingGradingByCourse(Long courseId);

    boolean existsByUserIdAndExerciseId(Long userId, Long exerciseId);

    @Query("SELECT DISTINCT ar.exerciseId FROM AnswerRecord ar WHERE ar.userId = :userId")
    List<Long> findDistinctExerciseIdsByUserId(Long userId);

    /** 用于计算 pkc 分母：该学生答过的不同习题数量 */
    @Query("SELECT COUNT(DISTINCT ar.exerciseId) FROM AnswerRecord ar WHERE ar.userId = :userId")
    long countDistinctExerciseIdsByUserId(Long userId);

    /** 按用户+习题ID批量查最近一次作答记录（用于计算 exfr） */
    @Query("SELECT ar FROM AnswerRecord ar WHERE ar.userId = :userId " +
           "AND ar.exerciseId IN :exerciseIds " +
           "AND ar.submittedAt = (SELECT MAX(ar2.submittedAt) FROM AnswerRecord ar2 " +
           "WHERE ar2.userId = :userId AND ar2.exerciseId = ar.exerciseId)")
    List<AnswerRecord> findLatestPerExercise(Long userId, List<Long> exerciseIds);
}
