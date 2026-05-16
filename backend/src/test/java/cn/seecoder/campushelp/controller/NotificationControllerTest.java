package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.LoginRequest;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.mapper.NotificationMapper;
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
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        notificationMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

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

        authToken = objectMapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    @DisplayName("GET /notifications without token returns 403")
    void list_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /notifications returns empty list initially")
    void list_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /notifications/unread-count returns 0 initially")
    void unreadCount_shouldReturnZero() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    @DisplayName("Notifications are created when demand is accepted")
    void accept_shouldCreateNotification() throws Exception {
        // Publish a demand
        String pubBody = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("type", "errand", "title", "需要被接的单", "description", "测试"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int demandId = objectMapper.readTree(pubBody).path("data").path("demandId").asInt();

        // Create and login another user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId("helper02");
        regReq.setPassword("pass123");
        regReq.setName("帮手");
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId("helper02");
        loginReq.setPassword("pass123");
        String helperBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String helperToken = objectMapper.readTree(helperBody).path("data").path("token").asText();

        // Helper accepts
        mockMvc.perform(put("/api/v1/demands/" + demandId + "/accept")
                        .header("Authorization", "Bearer " + helperToken))
                .andExpect(status().isOk());

        // Publisher should have a notification
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("ACCEPT"));
    }

    @Test
    @DisplayName("PUT /notifications/read-all marks all as read")
    void markAllRead_shouldClearUnread() throws Exception {
        // Trigger a notification via accept flow
        String pubBody = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("type", "errand", "title", "测试通知", "description", "测试"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int demandId = objectMapper.readTree(pubBody).path("data").path("demandId").asInt();

        // Register helper and accept to trigger notification
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId("helper03"); regReq.setPassword("pass123"); regReq.setName("帮");
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq))).andExpect(status().isOk());
        LoginRequest lr = new LoginRequest(); lr.setStudentId("helper03"); lr.setPassword("pass123");
        String hBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lr))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String hTok = objectMapper.readTree(hBody).path("data").path("token").asText();

        mockMvc.perform(put("/api/v1/demands/" + demandId + "/accept")
                        .header("Authorization", "Bearer " + hTok)).andExpect(status().isOk());

        // Mark all read
        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }
}
