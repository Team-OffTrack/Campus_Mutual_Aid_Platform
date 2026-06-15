package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.CreateReportRequest;
import cn.seecoder.campushelp.dto.LoginRequest;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.ReportMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DemandMapper demandMapper;

    @Autowired
    private ReportMapper reportMapper;

    private String authToken;
    private String adminToken;
    private Long publishedDemandId;
    private Long reporterUserId;

    @BeforeEach
    void setUp() throws Exception {
        reportMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Register and login regular user
        authToken = registerAndLogin("rpttest", "举报测试");

        // Get the user ID
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "rpttest"));
        reporterUserId = user.getUserId();

        // Publish a demand
        CreateDemandRequest demandReq = new CreateDemandRequest();
        demandReq.setType("errand");
        demandReq.setTitle("举报测试需求");
        demandReq.setDescription("用于举报测试");
        demandReq.setRewardType("point");
        demandReq.setRewardAmount(10);

        String demandBody = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demandReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        publishedDemandId = objectMapper.readTree(demandBody).path("data").path("demandId").asLong();

        // Register and promote an admin user
        String adminRegToken = registerAndLogin("rptadmin", "举报管理员");
        User adminUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "rptadmin"));
        adminUser.setRole("ADMIN");
        userMapper.updateById(adminUser);

        // Re-login as admin to get a token with ADMIN role
        LoginRequest adminLoginReq = new LoginRequest();
        adminLoginReq.setStudentId("rptadmin");
        adminLoginReq.setPassword("pass123");
        String adminBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminBody).path("data").path("token").asText();
    }

    private String registerAndLogin(String studentId, String name) throws Exception {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId(studentId);
        regReq.setPassword("pass123");
        regReq.setName(name);
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId(studentId);
        loginReq.setPassword("pass123");
        String body = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    @DisplayName("POST /reports should return 200")
    void createReport_shouldReturn200() throws Exception {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("MISLEADING");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /reports without auth returns 403")
    void createReport_withoutAuth_shouldReturn403() throws Exception {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("MISLEADING");

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /reports with invalid targetType returns 400")
    void createReport_invalidTargetType_shouldReturn400() throws Exception {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("INVALID");
        req.setTargetId(publishedDemandId);
        req.setReason("MISLEADING");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /reports with invalid reason returns 400")
    void createReport_invalidReason_shouldReturn400() throws Exception {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("INVALID_REASON");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /reports on non-existent target returns 404")
    void createReport_nonExistentTarget_shouldReturn404() throws Exception {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(99999L);
        req.setReason("SPAM");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /reports duplicate pending returns 409")
    void createReport_duplicatePending_shouldReturn409() throws Exception {
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("SPAM");

        // First report
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Duplicate
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("GET /admin/reports should return paginated list for admin")
    void adminListReports_shouldReturnPaginated() throws Exception {
        // Create a report first
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("HARASSMENT");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].reason").value("HARASSMENT"))
                .andExpect(jsonPath("$.data.records[0].targetType").value("DEMAND"))
                .andExpect(jsonPath("$.data.records[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /admin/reports should filter by status")
    void adminListReports_shouldFilterByStatus() throws Exception {
        // Create a report and resolve it
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("ILLEGAL");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Filter by RESOLVED should return 0
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        // Filter by PENDING should return 1
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("GET /admin/reports with non-admin returns 403")
    void adminListReports_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /admin/reports/{id}/resolve should return 200")
    void adminResolveReport_shouldReturn200() throws Exception {
        // Create a report
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("OTHER");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Get report ID from list
        String listBody = mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long reportId = objectMapper.readTree(listBody).path("data").path("records").get(0).path("id").asLong();

        // Resolve it as admin
        String resolveJson = "{\"status\":\"RESOLVED\",\"adminNote\":\"已处理\"}";
        mockMvc.perform(put("/api/v1/admin/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /admin/reports/{id}/resolve on already resolved returns 400")
    void adminResolveReport_alreadyResolved_shouldReturn400() throws Exception {
        // Create a report
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("OTHER");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Get report ID from list
        String listBody = mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long reportId = objectMapper.readTree(listBody).path("data").path("records").get(0).path("id").asLong();

        String resolveJson = "{\"status\":\"RESOLVED\"}";
        // First resolve
        mockMvc.perform(put("/api/v1/admin/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isOk());

        // Second resolve should fail
        mockMvc.perform(put("/api/v1/admin/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PUT /admin/reports/{id}/resolve with DELETE_DEMAND cancels the demand")
    void adminResolveReport_deleteDemand_shouldCancelDemand() throws Exception {
        // Create a report
        CreateReportRequest req = new CreateReportRequest();
        req.setTargetType("DEMAND");
        req.setTargetId(publishedDemandId);
        req.setReason("ILLEGAL");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Get report ID
        String listBody = mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long reportId = objectMapper.readTree(listBody).path("data").path("records").get(0).path("id").asLong();

        // Resolve with DELETE_DEMAND
        String resolveJson = "{\"status\":\"RESOLVED\",\"action\":\"DELETE_DEMAND\"}";
        mockMvc.perform(put("/api/v1/admin/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify demand is now cancelled
        mockMvc.perform(get("/api/v1/demands/" + publishedDemandId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
