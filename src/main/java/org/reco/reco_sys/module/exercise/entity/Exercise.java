package org.reco.reco_sys.module.exercise.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "answer_key", columnDefinition = "LONGTEXT")
    private String answerKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(name = "creator_id")
    private Long creatorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Type {
        SINGLE_CHOICE, MULTIPLE_CHOICE, FILL_BLANK, SHORT_ANSWER
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
}
