package org.reco.reco_sys.module.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private Long relatedId;
    private LocalDateTime createdAt;
}
