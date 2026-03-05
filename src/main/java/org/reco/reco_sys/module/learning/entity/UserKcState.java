package org.reco.reco_sys.module.learning.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_kc_state",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "kp_id"}))
public class UserKcState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kp_id", nullable = false)
    private Long kpId;

    @Column(name = "mastery_level", nullable = false)
    private Double masteryLevel = 0.0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
