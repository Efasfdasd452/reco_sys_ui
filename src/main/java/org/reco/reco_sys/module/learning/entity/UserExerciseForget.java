package org.reco.reco_sys.module.learning.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_exercise_forget",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "exercise_id"}))
public class UserExerciseForget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "forget_prob", nullable = false)
    private Double forgetProb = 0.0;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
