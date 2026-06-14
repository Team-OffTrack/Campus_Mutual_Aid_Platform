package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.LoginRequest;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.FavoriteMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DemandMapper demandMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    private String authToken;
    private Long publishedDemandId;

    @BeforeEach
    void setUp() throws Exception {
        favoriteMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Register and login test user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId("favtest");
        regReq.setPassword("pass123");
        regReq.setName("收藏测试");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId("favtest");
        loginReq.setPassword("pass123");

        String body = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        authToken = objectMapper.readTree(body).path("data").path("token").asText();

        // Publish a demand for tests
        CreateDemandRequest demandReq = new CreateDemandRequest();
        demandReq.setType("errand");
        demandReq.setTitle("收藏测试需求");
        demandReq.setDescription("用于收藏测试");
        demandReq.setRewardType("point");
        demandReq.setRewardAmount(10);

        String demandBody = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demandReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        publishedDemandId = objectMapper.readTree(demandBody).path("data").path("demandId").asLong();
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
    @DisplayName("POST /demands/{id}/favorite should return 200")
    void favorite_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /demands/{id}/favorite on non-existent demand returns 404")
    void favorite_nonExistentDemand_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/v1/demands/99999/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /demands/{id}/favorite twice returns 400")
    void favorite_duplicate_shouldReturn400() throws Exception {
        // First favorite
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Duplicate
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("DELETE /demands/{id}/favorite should return 200")
    void unfavorite_shouldReturn200() throws Exception {
        // Favorite first
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Then unfavorite
        mockMvc.perform(delete("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /demands/{id}/favorite on non-favorited demand is idempotent")
    void unfavorite_idempotent_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /demands/my/favorites returns paginated list")
    void listFavorites_shouldReturnPaginated() throws Exception {
        // Favorite a demand
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/demands/my/favorites")
                        .header("Authorization", "Bearer " + authToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].demandId").value(publishedDemandId))
                .andExpect(jsonPath("$.data.records[0].title").value("收藏测试需求"));
    }

    @Test
    @DisplayName("GET /demands/my/favorites without auth returns 403")
    void listFavorites_withoutAuth_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/demands/my/favorites"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /demands/{id} shows favorited=true when favorited")
    void detail_showsFavoritedTrue_whenFavorited() throws Exception {
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/demands/" + publishedDemandId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    @DisplayName("GET /demands/{id} shows favorited=false when not favorited")
    void detail_showsFavoritedFalse_whenNotFavorited() throws Exception {
        mockMvc.perform(get("/api/v1/demands/" + publishedDemandId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    @DisplayName("GET /demands list shows favorited flag for authenticated user")
    void list_showsFavoritedFlag() throws Exception {
        // Favorite the demand
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // List should show favorited=true
        mockMvc.perform(get("/api/v1/demands")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].favorited").value(true));
    }

    @Test
    @DisplayName("Favorites list includes cancelled demand")
    void favorites_includeCancelledDemand() throws Exception {
        // Favorite the demand
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Cancel the demand
        mockMvc.perform(put("/api/v1/demands/" + publishedDemandId + "/cancel")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Favorites list should still include it
        mockMvc.perform(get("/api/v1/demands/my/favorites")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("CANCELLED"));
    }

    @Test
    @DisplayName("favorite without token returns 403")
    void favorite_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/demands/" + publishedDemandId + "/favorite"))
                .andExpect(status().isForbidden());
    }
}
