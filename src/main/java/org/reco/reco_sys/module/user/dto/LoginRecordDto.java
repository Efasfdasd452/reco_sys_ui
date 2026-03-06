package org.reco.reco_sys.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginRecordDto {
    private Long id;
    private String ipAddress;
    private String location;
    private String userAgent;
    private Boolean isAnomaly;
    private LocalDateTime loginAt;
}
