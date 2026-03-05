package org.reco.reco_sys.module.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "login_record")
public class LoginRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String location;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "is_anomaly")
    private Boolean isAnomaly = false;

    @CreationTimestamp
    @Column(name = "login_at", updatable = false)
    private LocalDateTime loginAt;
}
