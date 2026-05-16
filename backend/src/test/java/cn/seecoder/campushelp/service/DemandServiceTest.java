package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
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
class DemandServiceTest {

    @Autowired
    private DemandService demandService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DemandMapper demandMapper;

    private Long publisherId;

    @BeforeEach
    void setUp() {
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Create a test user to act as publisher
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("pub001");
        req.setPassword("pass123");
        req.setName("发布者");
        userService.register(req);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "pub001"));
        publisherId = user.getUserId();
    }

    @Test
    @DisplayName("Publish demand creates OPEN demand with correct fields")
    void publish_shouldCreateDemand() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("帮忙取快递");
        req.setDescription("从东门快递站取包裹送到宿舍楼");
        req.setLocation("东门快递站");
        req.setRewardType("point");
        req.setRewardAmount(50);

        DemandResponse rsp = demandService.publish(publisherId, req);
        assertNotNull(rsp.getDemandId());
        assertEquals("errand", rsp.getType());
        assertEquals("帮忙取快递", rsp.getTitle());
        assertEquals("OPEN", rsp.getStatus());
        assertEquals("发布者", rsp.getPublisherName());
        assertEquals(50, rsp.getRewardAmount());
    }

    @Test
    @DisplayName("Publish anonymously hides publisher name")
    void publish_anonymous_shouldHideName() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("trade");
        req.setTitle("卖二手书");
        req.setDescription("数据结构教材9成新");
        req.setIsAnonymous(true);

        DemandResponse rsp = demandService.publish(publisherId, req);
        assertEquals("匿名用户", rsp.getPublisherName());
        assertTrue(rsp.getIsAnonymous());
    }

    @Test
    @DisplayName("List demands filters by type")
    void list_shouldFilterByType() {
        for (int i = 1; i <= 3; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType(i <= 2 ? "errand" : "trade");
            req.setTitle("需求" + i);
            req.setDescription("描述" + i);
            demandService.publish(publisherId, req);
        }

        var page = demandService.list(1, 10, "errand", null, null);
        assertEquals(2, page.getTotal());
        page.getRecords().forEach(r -> assertEquals("errand", r.getType()));
    }

    @Test
    @DisplayName("List demands filters by keyword")
    void list_shouldFilterByKeyword() {
        CreateDemandRequest req1 = new CreateDemandRequest();
        req1.setType("errand");
        req1.setTitle("取快递");
        req1.setDescription("快递");
        demandService.publish(publisherId, req1);

        CreateDemandRequest req2 = new CreateDemandRequest();
        req2.setType("errand");
        req2.setTitle("买奶茶");
        req2.setDescription("帮买奶茶");
        demandService.publish(publisherId, req2);

        var page = demandService.list(1, 10, null, "快递", null);
        assertEquals(1, page.getTotal());
        assertEquals("取快递", page.getRecords().get(0).getTitle());
    }

    @Test
    @DisplayName("Get demand by ID returns full detail")
    void getById_shouldReturnDetail() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("team");
        req.setTitle("组队刷题");
        req.setDescription("一起刷LeetCode");
        req.setLocation("图书馆");

        DemandResponse created = demandService.publish(publisherId, req);
        DemandResponse detail = demandService.getById(created.getDemandId());

        assertEquals(created.getDemandId(), detail.getDemandId());
        assertEquals("组队刷题", detail.getTitle());
        assertEquals("图书馆", detail.getLocation());
    }

    @Test
    @DisplayName("Get non-existent demand throws not found")
    void getById_nonExistent_shouldThrow() {
        assertThrows(BusinessException.class, () -> demandService.getById(99999L));
    }

    @Test
    @DisplayName("Cancel demand changes status to CANCELLED")
    void cancel_shouldChangeStatus() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("other");
        req.setTitle("测试取消");
        req.setDescription("测试");
        DemandResponse created = demandService.publish(publisherId, req);

        demandService.cancel(created.getDemandId(), publisherId);

        DemandResponse detail = demandService.getById(created.getDemandId());
        assertEquals("CANCELLED", detail.getStatus());
    }

    @Test
    @DisplayName("Cancel by non-publisher throws forbidden")
    void cancel_byOtherUser_shouldThrow() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("other");
        req.setTitle("别人的需求");
        req.setDescription("测试");
        DemandResponse created = demandService.publish(publisherId, req);

        // Create another user
        RegisterRequest reg = new RegisterRequest();
        reg.setStudentId("other001");
        reg.setPassword("pass123");
        reg.setName("其他人");
        userService.register(reg);
        User other = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "other001"));

        assertThrows(BusinessException.class,
                () -> demandService.cancel(created.getDemandId(), other.getUserId()));
    }

    @Test
    @DisplayName("List demands with sort by reward high")
    void list_sortByRewardHigh_shouldOrderByReward() {
        for (int i = 1; i <= 3; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("errand");
            req.setTitle("需求" + i);
            req.setDescription("描述" + i);
            req.setRewardAmount(i * 10);
            demandService.publish(publisherId, req);
        }

        var page = demandService.list(1, 10, null, null, "reward_high");
        assertEquals(3, page.getTotal());
        assertEquals(30, page.getRecords().get(0).getRewardAmount());
    }

    @Test
    @DisplayName("List demands with sort by reward low")
    void list_sortByRewardLow_shouldOrderAscending() {
        for (int i = 1; i <= 3; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("trade");
            req.setTitle("需求" + i);
            req.setDescription("描述" + i);
            req.setRewardAmount(i * 5);
            demandService.publish(publisherId, req);
        }

        var page = demandService.list(1, 10, null, null, "reward_low");
        assertEquals(3, page.getTotal());
        assertEquals(5, page.getRecords().get(0).getRewardAmount());
    }

    @Test
    @DisplayName("List demands with sort by deadline")
    void list_sortByDeadline_shouldOrderByDeadlineAsc() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 3; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("study");
            req.setTitle("需求" + i);
            req.setDescription("描述" + i);
            req.setDeadline(now.plusDays(i));
            demandService.publish(publisherId, req);
        }

        var page = demandService.list(1, 10, null, null, "deadline");
        assertEquals(3, page.getTotal());
        // First should have earliest deadline (now + 1 day)
        assertNotNull(page.getRecords().get(0).getDeadline());
    }

    @Test
    @DisplayName("Publish with all optional fields set")
    void publish_withAllFields_shouldPersistAll() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(3);
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("全字段需求");
        req.setDescription("包含所有可选字段的测试");
        req.setLocation("图书馆大门");
        req.setDeadline(deadline);
        req.setRewardType("cash");
        req.setRewardAmount(100);
        req.setIsAnonymous(false);

        DemandResponse rsp = demandService.publish(publisherId, req);
        DemandResponse detail = demandService.getById(rsp.getDemandId());

        assertEquals("图书馆大门", detail.getLocation());
        assertEquals("cash", detail.getRewardType());
        assertEquals(100, detail.getRewardAmount());
        assertFalse(detail.getIsAnonymous());
        assertEquals("发布者", detail.getPublisherName());
    }

    @Test
    @DisplayName("Cancel already cancelled demand throws")
    void cancel_alreadyCancelled_shouldThrow() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("other");
        req.setTitle("重复取消");
        req.setDescription("测试");
        DemandResponse created = demandService.publish(publisherId, req);

        demandService.cancel(created.getDemandId(), publisherId);
        assertThrows(BusinessException.class,
                () -> demandService.cancel(created.getDemandId(), publisherId));
    }

    @Test
    @DisplayName("List with no matching keyword returns empty page")
    void list_noMatchKeyword_shouldReturnEmpty() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("普通需求");
        req.setDescription("描述");
        demandService.publish(publisherId, req);

        var page = demandService.list(1, 10, null, "不存在的关键词XYZ", null);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    @DisplayName("List with unknown sort falls back to newest")
    void list_unknownSort_shouldFallbackToNewest() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("测试");
        req.setDescription("描述");
        demandService.publish(publisherId, req);

        // unknown sortBy should not throw
        var page = demandService.list(1, 10, null, null, "invalid_sort");
        assertEquals(1, page.getTotal());
    }
}
