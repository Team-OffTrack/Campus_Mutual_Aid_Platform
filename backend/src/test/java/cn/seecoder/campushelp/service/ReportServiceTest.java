package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.entity.Notification;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.NotificationMapper;
import cn.seecoder.campushelp.mapper.ReportMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private DemandService demandService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DemandMapper demandMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    private Long userId;
    private Long demandId;

    @BeforeEach
    void setUp() {
        reportMapper.delete(new LambdaQueryWrapper<>());
        notificationMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Create a test user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId("rptsvc");
        regReq.setPassword("pass123");
        regReq.setName("举报服务测试");
        userService.register(regReq);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "rptsvc"));
        userId = user.getUserId();

        // Publish a demand
        CreateDemandRequest demandReq = new CreateDemandRequest();
        demandReq.setType("errand");
        demandReq.setTitle("举报服务测试需求");
        demandReq.setDescription("测试描述");
        demandReq.setRewardType("point");
        demandReq.setRewardAmount(10);
        DemandResponse rsp = demandService.publish(userId, demandReq);
        demandId = rsp.getDemandId();
    }

    private CreateReportRequest buildRequest(String targetType, Long targetId, String reason) {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType(targetType);
        req.setTargetId(targetId);
        req.setReason(reason);
        return req;
    }

    @Test
    @DisplayName("createReport should insert a record")
    void createReport_shouldInsert() {
        CreateReportRequest req = buildRequest("DEMAND", demandId, "SPAM");
        reportService.createReport(userId, req);

        Page<ReportResponse> page = reportService.listReports(1, 10, "PENDING");
        assertEquals(1, page.getTotal());
        assertEquals("DEMAND", page.getRecords().get(0).getTargetType());
        assertEquals(demandId, page.getRecords().get(0).getTargetId());
        assertEquals("SPAM", page.getRecords().get(0).getReason());
    }

    @Test
    @DisplayName("createReport duplicate pending should throw BusinessException")
    void createReport_duplicatePending_shouldThrow() {
        CreateReportRequest req = buildRequest("DEMAND", demandId, "SPAM");
        reportService.createReport(userId, req);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.createReport(userId, req));
        assertEquals(409, ex.getCode());
    }

    @Test
    @DisplayName("createReport after resolved should succeed (not a duplicate)")
    void createReport_afterResolved_shouldSucceed() {
        CreateReportRequest req = buildRequest("DEMAND", demandId, "SPAM");
        reportService.createReport(userId, req);

        // Resolve the report
        Page<ReportResponse> page = reportService.listReports(1, 10, null);
        Long reportId = page.getRecords().get(0).getId();

        ResolveReportRequest resolveReq = new ResolveReportRequest();
        resolveReq.setStatus("RESOLVED");
        reportService.resolveReport(reportId, userId, resolveReq);

        // Create report again — should succeed
        assertDoesNotThrow(() -> reportService.createReport(userId, req));
    }

    @Test
    @DisplayName("createReport non-existent demand should throw NOT_FOUND")
    void createReport_nonExistentDemand_shouldThrow() {
        CreateReportRequest req = buildRequest("DEMAND", 99999L, "OTHER");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.createReport(userId, req));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("listReports should filter by status")
    void listReports_shouldFilterByStatus() {
        // Create 2 reports
        CreateReportRequest req1 = buildRequest("DEMAND", demandId, "SPAM");
        reportService.createReport(userId, req1);

        // Publish another demand and report it
        CreateDemandRequest demandReq2 = new CreateDemandRequest();
        demandReq2.setType("study");
        demandReq2.setTitle("第二个需求");
        demandReq2.setDescription("desc");
        demandReq2.setRewardType("point");
        DemandResponse rsp2 = demandService.publish(userId, demandReq2);

        CreateReportRequest req2 = buildRequest("DEMAND", rsp2.getDemandId(), "ILLEGAL");
        reportService.createReport(userId, req2);

        // Filter pending — should have 2
        Page<ReportResponse> pending = reportService.listReports(1, 10, "PENDING");
        assertEquals(2, pending.getTotal());

        // Filter resolved — should have 0
        Page<ReportResponse> resolved = reportService.listReports(1, 10, "RESOLVED");
        assertEquals(0, resolved.getTotal());
    }

    @Test
    @DisplayName("resolveReport should update status and set admin fields")
    void resolveReport_shouldUpdateStatus() {
        CreateReportRequest req = buildRequest("DEMAND", demandId, "ILLEGAL");
        reportService.createReport(userId, req);

        Page<ReportResponse> page = reportService.listReports(1, 10, null);
        Long reportId = page.getRecords().get(0).getId();

        ResolveReportRequest resolveReq = new ResolveReportRequest();
        resolveReq.setStatus("DISMISSED");
        resolveReq.setAdminNote("无违规");
        reportService.resolveReport(reportId, userId, resolveReq);

        // Verify
        Page<ReportResponse> dismissed = reportService.listReports(1, 10, "DISMISSED");
        assertEquals(1, dismissed.getTotal());
        ReportResponse resolved = dismissed.getRecords().get(0);
        assertEquals("DISMISSED", resolved.getStatus());
        assertEquals("无违规", resolved.getAdminNote());
        assertEquals(userId, resolved.getAdminId());
        assertNotNull(resolved.getResolveTime());
    }

    @Test
    @DisplayName("resolveReport non-existent should throw NOT_FOUND")
    void resolveReport_nonExistent_shouldThrow() {
        ResolveReportRequest resolveReq = new ResolveReportRequest();
        resolveReq.setStatus("RESOLVED");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.resolveReport(99999L, userId, resolveReq));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("createReport should notify all admins")
    void createReport_shouldNotifyAdmins() {
        // Create an admin user
        RegisterRequest adminReg = new RegisterRequest();
        adminReg.setStudentId("admin001");
        adminReg.setPassword("pass123");
        adminReg.setName("管理员");
        userService.register(adminReg);

        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "admin001"));
        admin.setRole("ADMIN");
        userMapper.updateById(admin);

        // Create a report
        CreateReportRequest req = buildRequest("DEMAND", demandId, "SPAM");
        reportService.createReport(userId, req);

        // Admin should have an unread notification
        long unread = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, admin.getUserId())
                        .eq(Notification::getIsRead, 0));
        assertTrue(unread > 0, "Admin should receive notification for new report");
    }

    @Test
    @DisplayName("resolveReport with DELETE_DEMAND should cancel the demand")
    void resolveReport_deleteDemand_shouldCancelDemand() {
        // Create a report on the demand
        CreateReportRequest req = buildRequest("DEMAND", demandId, "SPAM");
        reportService.createReport(userId, req);

        Page<ReportResponse> page = reportService.listReports(1, 10, null);
        Long reportId = page.getRecords().get(0).getId();

        // Resolve with DELETE_DEMAND action
        ResolveReportRequest resolveReq = new ResolveReportRequest();
        resolveReq.setStatus("RESOLVED");
        resolveReq.setAction("DELETE_DEMAND");
        reportService.resolveReport(reportId, userId, resolveReq);

        // Demand should be cancelled
        DemandResponse demand = demandService.getById(demandId);
        assertEquals("CANCELLED", demand.getStatus());
    }

    @Test
    @DisplayName("resolveReport with BAN_USER should set user status to 0")
    void resolveReport_banUser_shouldSetStatusZero() {
        // Create a second user to report
        RegisterRequest targetReg = new RegisterRequest();
        targetReg.setStudentId("target001");
        targetReg.setPassword("pass123");
        targetReg.setName("目标用户");
        userService.register(targetReg);

        User targetUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "target001"));
        assertEquals(1, targetUser.getStatus());

        // Report the target user
        CreateReportRequest req = buildRequest("USER", targetUser.getUserId(), "HARASSMENT");
        reportService.createReport(userId, req);

        Page<ReportResponse> page = reportService.listReports(1, 10, null);
        Long reportId = page.getRecords().get(0).getId();

        // Resolve with BAN_USER action
        ResolveReportRequest resolveReq = new ResolveReportRequest();
        resolveReq.setStatus("RESOLVED");
        resolveReq.setAction("BAN_USER");
        reportService.resolveReport(reportId, userId, resolveReq);

        // Target user should be banned
        User banned = userMapper.selectById(targetUser.getUserId());
        assertEquals(0, banned.getStatus());
    }

    @Test
    @DisplayName("resolveReport with BAN_USER on DEMAND target should be ignored")
    void resolveReport_wrongActionForTarget_shouldSkip() {
        // Report a demand
        CreateReportRequest req = buildRequest("DEMAND", demandId, "ILLEGAL");
        reportService.createReport(userId, req);

        Page<ReportResponse> page = reportService.listReports(1, 10, null);
        Long reportId = page.getRecords().get(0).getId();

        // Resolve with BAN_USER on a DEMAND report — action should be ignored
        ResolveReportRequest resolveReq = new ResolveReportRequest();
        resolveReq.setStatus("RESOLVED");
        resolveReq.setAction("BAN_USER");  // Wrong action for DEMAND target
        assertDoesNotThrow(() -> reportService.resolveReport(reportId, userId, resolveReq));

        // Demand should NOT be cancelled (action was ignored)
        DemandResponse demand = demandService.getById(demandId);
        assertEquals("OPEN", demand.getStatus());
    }
}
