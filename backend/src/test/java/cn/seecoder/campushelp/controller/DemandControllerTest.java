package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.LoginRequest;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
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
class DemandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DemandMapper demandMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Register and login a test user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId("test001");
        regReq.setPassword("pass123");
        regReq.setName("测试用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId("test001");
        loginReq.setPassword("pass123");

        String body = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(body);
        authToken = node.path("data").path("token").asText();
    }

    @Test
    @DisplayName("POST /demands without token returns 403")
    void publish_withoutToken_shouldReturn403() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("测试");
        req.setDescription("描述");

        mockMvc.perform(post("/api/v1/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /demands with valid body returns created demand")
    void publish_validBody_shouldReturnDemand() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("帮忙取快递");
        req.setDescription("从东门快递站取包裹");
        req.setLocation("东门");
        req.setRewardType("point");
        req.setRewardAmount(30);

        mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("errand"))
                .andExpect(jsonPath("$.data.title").value("帮忙取快递"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /demands with missing title returns 400")
    void publish_missingTitle_shouldReturn400() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setDescription("描述");
        // title is missing

        mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /demands returns paginated list")
    void list_shouldReturnPage() throws Exception {
        // Publish 2 demands first
        for (int i = 1; i <= 2; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("errand");
            req.setTitle("需求" + i);
            req.setDescription("描述" + i);
            mockMvc.perform(post("/api/v1/demands")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    @DisplayName("GET /demands with type filter returns filtered results")
    void list_withTypeFilter_shouldFilter() throws Exception {
        for (String type : new String[]{"errand", "trade", "errand"}) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType(type);
            req.setTitle("需求-" + type);
            req.setDescription("描述");
            mockMvc.perform(post("/api/v1/demands")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .param("type", "errand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @DisplayName("GET /demands/{id} returns demand detail")
    void getById_shouldReturnDetail() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("team");
        req.setTitle("组队学习");
        req.setDescription("一起复习期末");
        req.setLocation("图书馆三楼");

        String body = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int demandId = objectMapper.readTree(body).path("data").path("demandId").asInt();

        mockMvc.perform(get("/api/v1/demands/" + demandId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("组队学习"))
                .andExpect(jsonPath("$.data.location").value("图书馆三楼"));
    }

    @Test
    @DisplayName("PUT /demands/{id}/cancel cancels the demand")
    void cancel_shouldCancelDemand() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("other");
        req.setTitle("待取消需求");
        req.setDescription("测试取消");

        String body = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int demandId = objectMapper.readTree(body).path("data").path("demandId").asInt();

        mockMvc.perform(put("/api/v1/demands/" + demandId + "/cancel")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify status changed
        mockMvc.perform(get("/api/v1/demands/" + demandId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /demands with sortBy parameter returns sorted")
    void list_withSortBy_shouldSort() throws Exception {
        for (int i = 1; i <= 3; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("errand");
            req.setTitle("需求" + i);
            req.setDescription("描述" + i);
            req.setRewardAmount(i * 10);
            mockMvc.perform(post("/api/v1/demands")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .param("sortBy", "reward_high"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].rewardAmount").value(30));
    }

    @Test
    @DisplayName("GET /demands with keyword search filters correctly")
    void list_withKeyword_shouldFilter() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("帮忙取快递包裹");
        req.setDescription("从东门取");
        mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Search shouldn't match
        mockMvc.perform(get("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .param("keyword", "奶茶"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("GET /demands/{id} for non-existent returns error")
    void getById_nonExistent_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/v1/demands/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ── Order flow tests ──

    /** Helper: register + login another user, return their token. */
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
    @DisplayName("PUT /demands/{id}/accept transitions to IN_PROGRESS")
    void accept_shouldSetAcceptor() throws Exception {
        // Publish as test001
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("可接需求");
        req.setDescription("测试接单");
        String body = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int demandId = objectMapper.readTree(body).path("data").path("demandId").asInt();

        // Another user accepts
        String otherToken = registerAndLogin("acc001", "接单者");
        mockMvc.perform(put("/api/v1/demands/" + demandId + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.acceptorName").value("接单者"));
    }

    @Test
    @DisplayName("PUT /demands/{id}/accept on own demand returns error")
    void accept_ownDemand_shouldReturnError() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("自己的需求");
        req.setDescription("测试");
        String body = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int demandId = objectMapper.readTree(body).path("data").path("demandId").asInt();

        mockMvc.perform(put("/api/v1/demands/" + demandId + "/accept")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /demands/{id}/complete marks demand as COMPLETED")
    void complete_shouldFinishOrder() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("待完成");
        req.setDescription("测试");
        String body = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int demandId = objectMapper.readTree(body).path("data").path("demandId").asInt();

        // Accept first
        String otherToken = registerAndLogin("acc002", "接单人");
        mockMvc.perform(put("/api/v1/demands/" + demandId + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());

        // Publisher completes
        mockMvc.perform(put("/api/v1/demands/" + demandId + "/complete")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /demands/my returns user's orders")
    void myOrders_shouldReturnFiltered() throws Exception {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("我的发布项");
        req.setDescription("测试");
        mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/demands/my")
                        .header("Authorization", "Bearer " + authToken)
                        .param("role", "publisher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("我的发布项"));
    }
}
