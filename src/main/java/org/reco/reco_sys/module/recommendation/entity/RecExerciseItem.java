package org.reco.reco_sys.module.recommendation.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "rec_exercise_item")
public class RecExerciseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rec_id", nullable = false)
    private Long recId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
