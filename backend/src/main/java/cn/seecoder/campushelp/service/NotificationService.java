package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.entity.Notification;
import java.util.List;

/**
 * Manages in-app notifications triggered by demand lifecycle events.
 * <p>
 * Templates produce Chinese messages for ACCEPT, COMPLETE, and CANCEL events.
 * Notifications are per-user and include a linkable demand ID for navigation.
 */
public interface NotificationService {

    /** Send a notification to a specific user. */
    Notification create(Long userId, String type, String title, String content, Long relatedDemandId);

    /** Send ACCEPT notification to the demand publisher. */
    void notifyDemandAccepted(Long publisherId, String demandTitle, Long demandId, String acceptorName);

    /** Send COMPLETE notification to the demand publisher. */
    void notifyDemandCompleted(Long publisherId, String demandTitle, Long demandId);

    /** Send CANCEL notification to the other party (acceptor, if exists). */
    void notifyDemandCancelled(Long targetUserId, String demandTitle, Long demandId);

    /** List notifications for a user, newest first. */
    List<Notification> listByUser(Long userId);

    /** Mark a single notification as read. */
    void markRead(Long notificationId, Long userId);

    /** Mark all notifications as read for a user. */
    void markAllRead(Long userId);

    /** Count unread notifications for a user. */
    long unreadCount(Long userId);
}
