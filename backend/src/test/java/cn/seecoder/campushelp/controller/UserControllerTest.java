package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.LoginRequest;
import cn.seecoder.campushelp.dto.RegisterRequest;
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean DB to avoid duplicate-key conflicts across test methods
        userMapper.delete(new LambdaQueryWrapper<>());

        RegisterRequest req = new RegisterRequest();
        req.setStudentId("test001");
        req.setPassword("pass123");
        req.setName("测试用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
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
    @DisplayName("POST /user/register with valid body returns 200")
    void register_validBody_shouldReturn200() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("test002");
        req.setPassword("pass456");
        req.setName("新用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /user/register with duplicate studentId returns 400")
    void register_duplicate_shouldReturn400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("test001");
        req.setPassword("pass456");
        req.setName("重复用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("POST /user/login returns token")
    void login_shouldReturnToken() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId("test001");
        loginReq.setPassword("pass123");

        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.name").value("测试用户"));
    }

    @Test
    @DisplayName("GET /user/profile without token returns 403")
    void getProfile_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /user/profile with valid token returns user info")
    void getProfile_withToken_shouldReturnInfo() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试用户"))
                .andExpect(jsonPath("$.data.availablePoints").value(0));
    }

    @Test
    @DisplayName("PUT /user/profile updates name")
    void updateProfile_shouldUpdate() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("name", "新名字"));

        mockMvc.perform(put("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新名字"));
    }

    @Test
    @DisplayName("GET /admin/users without admin role returns 403")
    void listUsers_asNormalUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }
}
