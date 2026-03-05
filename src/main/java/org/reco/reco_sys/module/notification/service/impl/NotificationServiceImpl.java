package org.reco.reco_sys.module.notification.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.module.notification.dto.NotificationDto;
import org.reco.reco_sys.module.notification.entity.Notification;
import org.reco.reco_sys.module.notification.repository.NotificationRepository;
import org.reco.reco_sys.module.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Page<NotificationDto> list(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Override
    @Transactional
    public void send(Long userId, Notification.Type type, String title, String content, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        notificationRepository.save(n);
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType().name());
        dto.setTitle(n.getTitle());
        dto.setContent(n.getContent());
        dto.setIsRead(n.getIsRead());
        dto.setRelatedId(n.getRelatedId());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
