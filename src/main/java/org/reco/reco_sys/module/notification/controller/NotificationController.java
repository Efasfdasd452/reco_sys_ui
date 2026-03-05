package org.reco.reco_sys.module.notification.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.notification.dto.NotificationDto;
import org.reco.reco_sys.module.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<Page<NotificationDto>> list(@RequestHeader("Authorization") String token,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(notificationService.list(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(notificationService.countUnread(userId));
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        notificationService.markAllRead(userId);
        return Result.success(null);
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
