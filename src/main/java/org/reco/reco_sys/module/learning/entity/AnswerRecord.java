package org.reco.reco_sys.module.learning.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "answer_record")
public class AnswerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(columnDefinition = "LONGTEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SUBMITTED;

    @Column
    private Integer score;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "graded_by")
    private Long gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "time_spent")
    private Integer timeSpent;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        SUBMITTED, GRADING, GRADED, AUTO_GRADED
    }
}
