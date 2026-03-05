package org.reco.reco_sys.module.exercise.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "exercise_kp_rel",
        uniqueConstraints = @UniqueConstraint(columnNames = {"exercise_id", "kp_id"}))
public class ExerciseKpRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "kp_id", nullable = false)
    private Long kpId;

    @Column
    private Double weight = 1.0;
}
