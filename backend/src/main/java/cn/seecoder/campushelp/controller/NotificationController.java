package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.entity.Notification;
import cn.seecoder.campushelp.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * In-app notification endpoints.
 * <p>
 * All routes require authentication. Notifications are scoped to the current user.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** List all notifications for the current user, newest first. */
    @GetMapping
    public ApiResult<List<Notification>> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(notificationService.listByUser(userId));
    }

    /** Count unread notifications for badge display. */
    @GetMapping("/unread-count")
    public ApiResult<Map<String, Long>> unreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(Map.of("count", notificationService.unreadCount(userId)));
    }

    /** Mark all notifications as read. Must come before /{id}/read to avoid route ambiguity. */
    @PutMapping("/read-all")
    public ApiResult<Void> markAllRead(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markAllRead(userId);
        return ApiResult.success();
    }

    /** Mark a single notification as read. */
    @PutMapping("/{notificationId}/read")
    public ApiResult<Void> markRead(Authentication auth, @PathVariable Long notificationId) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markRead(notificationId, userId);
        return ApiResult.success();
    }
}
