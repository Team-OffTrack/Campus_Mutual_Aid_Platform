package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.entity.Notification;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.NotificationMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        notificationMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        RegisterRequest req = new RegisterRequest();
        req.setStudentId("notif001");
        req.setPassword("pass123");
        req.setName("通知用户");
        userService.register(req);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "notif001"));
        userId = user.getUserId();
    }

    @Test
    @DisplayName("Create notification persists correctly")
    void create_shouldPersist() {
        Notification n = notificationService.create(userId, "ACCEPT",
                "有人接单", "您的需求已被接单", null);

        assertNotNull(n.getNotificationId());
        assertEquals(userId, n.getUserId());
        assertEquals("ACCEPT", n.getType());
        assertEquals("有人接单", n.getTitle());
        assertEquals(0, n.getIsRead());
    }

    @Test
    @DisplayName("List notifications returns user's notifications newest first")
    void listByUser_shouldReturnOrdered() {
        notificationService.create(userId, "ACCEPT", "通知1", "内容1", null);
        notificationService.create(userId, "COMPLETE", "通知2", "内容2", null);

        List<Notification> list = notificationService.listByUser(userId);
        assertEquals(2, list.size());
        // Newest first
        assertEquals("通知2", list.get(0).getTitle());
    }

    @Test
    @DisplayName("Unread count returns correct number")
    void unreadCount_shouldCountUnread() {
        notificationService.create(userId, "ACCEPT", "未读1", "", null);
        notificationService.create(userId, "COMPLETE", "未读2", "", null);

        assertEquals(2, notificationService.unreadCount(userId));
    }

    @Test
    @DisplayName("Mark read changes isRead to 1")
    void markRead_shouldSetRead() {
        Notification n = notificationService.create(userId, "ACCEPT", "测试", "内容", null);

        notificationService.markRead(n.getNotificationId(), userId);
        List<Notification> list = notificationService.listByUser(userId);
        assertEquals(1, list.get(0).getIsRead().intValue());
    }

    @Test
    @DisplayName("Mark all read changes all unread to read")
    void markAllRead_shouldMarkAll() {
        notificationService.create(userId, "ACCEPT", "未读1", "", null);
        notificationService.create(userId, "COMPLETE", "未读2", "", null);

        notificationService.markAllRead(userId);
        assertEquals(0, notificationService.unreadCount(userId));
    }

    @Test
    @DisplayName("Mark read on non-existent notification throws")
    void markRead_nonExistent_shouldThrow() {
        assertThrows(cn.seecoder.campushelp.common.BusinessException.class,
                () -> notificationService.markRead(99999L, userId));
    }

    @Test
    @DisplayName("Template: accept notification has correct content")
    void notifyDemandAccepted_shouldHaveCorrectContent() {
        notificationService.notifyDemandAccepted(userId, "取快递", null, "张三");

        List<Notification> list = notificationService.listByUser(userId);
        assertEquals(1, list.size());
        Notification n = list.get(0);
        assertEquals("ACCEPT", n.getType());
        assertTrue(n.getContent().contains("取快递"));
        assertTrue(n.getContent().contains("张三"));
        assertNull(n.getRelatedDemandId());
    }

    @Test
    @DisplayName("Template: complete notification has correct content")
    void notifyDemandCompleted_shouldHaveCorrectContent() {
        notificationService.notifyDemandCompleted(userId, "二手交易", null);

        List<Notification> list = notificationService.listByUser(userId);
        assertEquals(1, list.size());
        assertEquals("COMPLETE", list.get(0).getType());
        assertTrue(list.get(0).getContent().contains("二手交易"));
    }

    @Test
    @DisplayName("Template: cancel notification has correct content")
    void notifyDemandCancelled_shouldHaveCorrectContent() {
        notificationService.notifyDemandCancelled(userId, "组队需求", null);

        List<Notification> list = notificationService.listByUser(userId);
        assertEquals(1, list.size());
        assertEquals("CANCEL", list.get(0).getType());
        assertTrue(list.get(0).getContent().contains("组队需求"));
    }

    @Test
    @DisplayName("Unread count is 0 when all notifications are read")
    void unreadCount_allRead_shouldReturnZero() {
        notificationService.create(userId, "ACCEPT", "通知", "", null);
        notificationService.markAllRead(userId);
        assertEquals(0, notificationService.unreadCount(userId));
    }
}
