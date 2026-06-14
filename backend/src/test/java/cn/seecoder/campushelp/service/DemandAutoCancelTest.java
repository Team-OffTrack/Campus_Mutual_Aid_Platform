package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.Notification;
import cn.seecoder.campushelp.entity.PointsTransaction;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.UserAccount;
import cn.seecoder.campushelp.entity.enums.NotificationType;
import cn.seecoder.campushelp.entity.enums.PointsTransactionType;
import cn.seecoder.campushelp.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DemandAutoCancelTest {

    @Autowired
    private DemandService demandService;

    @Autowired
    private UserService userService;

    @Autowired
    private DemandMapper demandMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private PointsTransactionMapper pointsTransactionMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    private Long publisherId;

    @BeforeEach
    void setUp() {
        notificationMapper.delete(new LambdaQueryWrapper<>());
        pointsTransactionMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        RegisterRequest req = new RegisterRequest();
        req.setStudentId("pub001");
        req.setPassword("pass123");
        req.setName("发布者");
        userService.register(req);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "pub001"));
        publisherId = user.getUserId();
    }

    /**
     * Publish a demand with a future deadline, then expire it by setting deadline to past.
     * Uses donation reward type so no points are involved.
     */
    private Long publishAndExpire(String title, String type) {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType(type);
        req.setTitle(title);
        req.setDescription("测试描述");
        req.setRewardType("donation");
        req.setRewardAmount(0);
        req.setDeadline(LocalDateTime.now().plusDays(1));
        DemandResponse created = demandService.publish(publisherId, req);

        Demand d = demandMapper.selectById(created.getDemandId());
        d.setDeadline(LocalDateTime.now().minusDays(1));
        demandMapper.updateById(d);
        return created.getDemandId();
    }

    @Test
    @DisplayName("Auto-cancel expired OPEN demand changes status to CANCELLED")
    void autoCancelExpired_openDemand_shouldCancel() {
        Long demandId = publishAndExpire("过期需求", "errand");

        demandService.autoCancelExpired(demandId);

        Demand updated = demandMapper.selectById(demandId);
        assertEquals("CANCELLED", updated.getStatus());
    }

    @Test
    @DisplayName("Auto-cancel expired IN_PROGRESS demand changes status to CANCELLED")
    void autoCancelExpired_inProgressDemand_shouldCancel() {
        // Publish and accept
        Long demandId = publishAndExpire("进行中过期需求", "errand");

        RegisterRequest reg = new RegisterRequest();
        reg.setStudentId("acc001"); reg.setPassword("pass"); reg.setName("接单者");
        userService.register(reg);
        User acceptor = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "acc001"));
        demandService.accept(demandId, acceptor.getUserId());

        demandService.autoCancelExpired(demandId);

        Demand updated = demandMapper.selectById(demandId);
        assertEquals("CANCELLED", updated.getStatus());
    }

    @Test
    @DisplayName("Auto-cancel already COMPLETED demand does nothing (no-op)")
    void autoCancelExpired_completedDemand_shouldDoNothing() {
        Long demandId = publishAndExpire("已完成需求", "errand");

        // Accept and complete
        RegisterRequest reg = new RegisterRequest();
        reg.setStudentId("acc002"); reg.setPassword("pass"); reg.setName("接单者");
        userService.register(reg);
        User acceptor = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "acc002"));
        demandService.accept(demandId, acceptor.getUserId());
        demandService.complete(demandId, publisherId);

        demandService.autoCancelExpired(demandId);

        Demand updated = demandMapper.selectById(demandId);
        assertEquals("COMPLETED", updated.getStatus());
    }

    @Test
    @DisplayName("Auto-cancel already CANCELLED demand does nothing (no-op)")
    void autoCancelExpired_alreadyCancelled_shouldDoNothing() {
        Long demandId = publishAndExpire("已取消需求", "errand");

        demandService.cancel(demandId, publisherId);

        demandService.autoCancelExpired(demandId);

        Demand updated = demandMapper.selectById(demandId);
        assertEquals("CANCELLED", updated.getStatus());
    }

    @Test
    @DisplayName("Auto-cancel expired point demand unfreezes points")
    void autoCancelExpired_pointDemand_shouldUnfreezePoints() {
        // Publish with point reward to properly freeze points
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("积分过期需求");
        req.setDescription("测试积分解冻");
        req.setRewardType("point");
        req.setRewardAmount(50);
        req.setDeadline(LocalDateTime.now().plusDays(1));
        DemandResponse created = demandService.publish(publisherId, req);

        // Expire the demand
        Demand d = demandMapper.selectById(created.getDemandId());
        d.setDeadline(LocalDateTime.now().minusDays(1));
        demandMapper.updateById(d);

        // Verify points are frozen before auto-cancel
        UserAccount accountBefore = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, publisherId));
        assertEquals(50, accountBefore.getFrozenPoints());

        demandService.autoCancelExpired(created.getDemandId());

        // Verify points were unfrozen
        UserAccount accountAfter = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, publisherId));
        assertEquals(0, accountAfter.getFrozenPoints());

        // Verify CANCEL_REFUND transaction exists
        PointsTransaction tx = pointsTransactionMapper.selectOne(
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(PointsTransaction::getUserId, publisherId)
                        .eq(PointsTransaction::getType, PointsTransactionType.CANCEL_REFUND)
                        .orderByDesc(PointsTransaction::getCreateTime)
                        .last("LIMIT 1"));
        assertNotNull(tx);
        assertEquals(50, tx.getAmount());
    }

    @Test
    @DisplayName("Auto-cancel notifies publisher about expiry")
    void autoCancelExpired_shouldNotifyPublisher() {
        Long demandId = publishAndExpire("通知测试需求", "errand");

        demandService.autoCancelExpired(demandId);

        Notification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, publisherId)
                        .eq(Notification::getType, NotificationType.CANCEL)
                        .eq(Notification::getTitle, "需求已过期")
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 1"));
        assertNotNull(notification, "Publisher should receive expiry notification");
        assertTrue(notification.getContent().contains("通知测试需求"));
    }

    @Test
    @DisplayName("Auto-cancel notifies acceptor when demand is IN_PROGRESS")
    void autoCancelExpired_withAcceptor_shouldNotifyAcceptor() {
        Long demandId = publishAndExpire("接单者通知需求", "errand");

        RegisterRequest reg = new RegisterRequest();
        reg.setStudentId("acc003"); reg.setPassword("pass"); reg.setName("接单者3");
        userService.register(reg);
        User acceptor = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "acc003"));
        demandService.accept(demandId, acceptor.getUserId());

        demandService.autoCancelExpired(demandId);

        Notification accNotif = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, acceptor.getUserId())
                        .eq(Notification::getType, NotificationType.CANCEL)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 1"));
        assertNotNull(accNotif, "Acceptor should receive cancellation notification");
    }

    @Test
    @DisplayName("Auto-cancel non-existent demand throws")
    void autoCancelExpired_nonExistent_shouldThrow() {
        assertThrows(BusinessException.class,
                () -> demandService.autoCancelExpired(99999L));
    }
}
