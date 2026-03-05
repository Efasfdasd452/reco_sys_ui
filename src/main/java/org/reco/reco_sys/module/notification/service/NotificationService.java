package org.reco.reco_sys.module.notification.service;

import org.reco.reco_sys.module.notification.dto.NotificationDto;
import org.reco.reco_sys.module.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<NotificationDto> list(Long userId, Pageable pageable);
    long countUnread(Long userId);
    void markAllRead(Long userId);
    void send(Long userId, Notification.Type type, String title, String content, Long relatedId);
}
