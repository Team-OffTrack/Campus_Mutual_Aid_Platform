package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.entity.Notification;
import cn.seecoder.campushelp.entity.enums.NotificationType;
import cn.seecoder.campushelp.mapper.NotificationMapper;
import cn.seecoder.campushelp.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   SimpMessagingTemplate messagingTemplate) {
        this.notificationMapper = notificationMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public Notification create(Long userId, String type, String title, String content, Long relatedDemandId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        n.setRelatedDemandId(relatedDemandId);
        notificationMapper.insert(n);

        // Push to recipient via WebSocket (best-effort, dropped if recipient is offline)
        messagingTemplate.convertAndSendToUser(
                userId.toString(), "/queue/notifications", n);

        return n;
    }

    @Override
    public void notifyDemandAccepted(Long publisherId, String demandTitle, Long demandId, String acceptorName) {
        create(publisherId, NotificationType.ACCEPT,
                "有人接单了",
                "您的需求「" + demandTitle + "」已被 " + acceptorName + " 接单",
                demandId);
    }

    @Override
    public void notifyDemandCompleted(Long publisherId, String demandTitle, Long demandId) {
        create(publisherId, NotificationType.COMPLETE,
                "需求已完成",
                "您的需求「" + demandTitle + "」已确认完成",
                demandId);
    }

    @Override
    public void notifyDemandCancelled(Long targetUserId, String demandTitle, Long demandId) {
        create(targetUserId, NotificationType.CANCEL,
                "需求已取消",
                "需求「" + demandTitle + "」已被发布者取消",
                demandId);
    }

    @Override
    public void notifyTaskCompleted(Long acceptorId, String demandTitle, Long demandId) {
        create(acceptorId, NotificationType.COMPLETE,
                "任务已完成",
                "您承接的需求「" + demandTitle + "」已被发布者确认完成，去评价一下吧",
                demandId);
    }

    @Override
    public void notifyEvaluationReceived(Long targetUserId, String demandTitle, Long demandId,
                                          String evaluatorName, int rating) {
        create(targetUserId, NotificationType.EVALUATION,
                "收到新评价",
                evaluatorName + " 对需求「" + demandTitle + "」给出了 " + rating + " 星评价",
                demandId);
    }

    @Override
    public void notifyEvaluationUpdated(Long targetUserId, String demandTitle, Long demandId,
                                         String evaluatorName, int rating) {
        create(targetUserId, NotificationType.EVALUATION,
                "评价已更新",
                evaluatorName + " 修改了对需求「" + demandTitle + "」的评价（" + rating + " 星）",
                demandId);
    }

    @Override
    public List<Notification> listByUser(Long userId) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null || !n.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        unread.forEach(n -> n.setIsRead(1));
        // Batch update — MyBatis-Plus doesn't have a bulk-update for entities, so update one by one in a tx
        unread.forEach(notificationMapper::updateById);
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }
}
