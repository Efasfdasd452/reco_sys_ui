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

    /** 文档公式：mlkc = correctCount / totalCount */
    @Column(name = "mastery_level", nullable = false)
    private Double masteryLevel = 0.0;

    /** 涉及该KC的题目中答对数（mlkc 分子） */
    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    /**
     * 涉及该KC的题目总答题数（mlkc 分母 = pkc 分子）。
     * pkc = totalCount / 该学生答题总数
     */
    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
