package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.UserBadge;
import cn.seecoder.campushelp.entity.WornBadge;
import cn.seecoder.campushelp.mapper.UserBadgeMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.mapper.WornBadgeMapper;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BadgeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private UserBadgeMapper userBadgeMapper;
    @Autowired private WornBadgeMapper wornBadgeMapper;

    private String authToken;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        wornBadgeMapper.delete(new LambdaQueryWrapper<>());
        userBadgeMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Register and login
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId("badgectrl");
        regReq.setPassword("pass123");
        regReq.setName("徽章控制器测试");
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        String loginBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"badgectrl\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        authToken = objectMapper.readTree(loginBody).path("data").path("token").asText();
        userId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "badgectrl")).getUserId();
    }

    // ── GET /badges ──

    @Test
    @DisplayName("GET /api/v1/badges — returns 200 with 9 badges")
    void getUserBadges_shouldReturn9Badges() throws Exception {
        mockMvc.perform(get("/api/v1/badges")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(9)));
    }

    @Test
    @DisplayName("GET /api/v1/badges — returns 403 without auth")
    void getUserBadges_shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/badges"))
                .andExpect(status().isForbidden());
    }

    // ── POST /badges/wear/{badgeKey} ──

    @Test
    @DisplayName("POST /api/v1/badges/wear/{key} — wear earned badge")
    void wearBadge_shouldWearEarnedBadge() throws Exception {
        // Award FIRST_PUBLISH by inserting a badge record directly
        UserBadge badge = new UserBadge();
        badge.setUserId(userId);
        badge.setBadgeKey("FIRST_PUBLISH");
        userBadgeMapper.insert(badge);

        mockMvc.perform(post("/api/v1/badges/wear/FIRST_PUBLISH")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify worn badge in DB
        WornBadge worn = wornBadgeMapper.selectOne(
                new LambdaQueryWrapper<WornBadge>().eq(WornBadge::getUserId, userId));
        assertNotNull(worn);
        assertEquals("FIRST_PUBLISH", worn.getBadgeKey());
    }

    @Test
    @DisplayName("POST /api/v1/badges/wear/{key} — 400 for unearned badge")
    void wearBadge_shouldReturn400ForUnearned() throws Exception {
        mockMvc.perform(post("/api/v1/badges/wear/HELPER")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/badges/wear/{key} — 403 without auth")
    void wearBadge_shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/badges/wear/FIRST_PUBLISH"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /badges/wear ──

    @Test
    @DisplayName("DELETE /api/v1/badges/wear — removes worn badge")
    void unwearBadge_shouldRemoveWorn() throws Exception {
        // First award and wear a badge
        UserBadge badge = new UserBadge();
        badge.setUserId(userId);
        badge.setBadgeKey("FIRST_PUBLISH");
        userBadgeMapper.insert(badge);

        WornBadge worn = new WornBadge();
        worn.setUserId(userId);
        worn.setBadgeKey("FIRST_PUBLISH");
        wornBadgeMapper.insert(worn);

        mockMvc.perform(delete("/api/v1/badges/wear")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify worn badge removed
        Long count = wornBadgeMapper.selectCount(
                new LambdaQueryWrapper<WornBadge>().eq(WornBadge::getUserId, userId));
        assertEquals(0L, count);
    }

    @Test
    @DisplayName("DELETE /api/v1/badges/wear — idempotent when nothing worn")
    void unwearBadge_shouldBeIdempotent() throws Exception {
        mockMvc.perform(delete("/api/v1/badges/wear")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
    }

    // ── POST /badges/easter-egg ──

    @Test
    @DisplayName("POST /api/v1/badges/easter-egg — awards badge")
    void awardEasterEgg_shouldAward() throws Exception {
        mockMvc.perform(post("/api/v1/badges/easter-egg")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Long count = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeKey, "EASTER_EGG"));
        assertEquals(1L, count);
    }

    @Test
    @DisplayName("POST /api/v1/badges/easter-egg — idempotent")
    void awardEasterEgg_shouldBeIdempotent() throws Exception {
        mockMvc.perform(post("/api/v1/badges/easter-egg")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/badges/easter-egg")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        Long count = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeKey, "EASTER_EGG"));
        assertEquals(1L, count);
    }

    @Test
    @DisplayName("POST /api/v1/badges/easter-egg — 403 without auth")
    void awardEasterEgg_shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/badges/easter-egg"))
                .andExpect(status().isForbidden());
    }

    // ── Profile includes wornBadgeKey ──

    @Test
    @DisplayName("GET /api/v1/user/profile — includes wornBadgeKey when wearing")
    void getProfile_shouldIncludeWornBadgeKey() throws Exception {
        // Award and wear a badge
        UserBadge badge = new UserBadge();
        badge.setUserId(userId);
        badge.setBadgeKey("FIRST_PUBLISH");
        userBadgeMapper.insert(badge);

        WornBadge worn = new WornBadge();
        worn.setUserId(userId);
        worn.setBadgeKey("FIRST_PUBLISH");
        wornBadgeMapper.insert(worn);

        mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.wornBadgeKey").value("FIRST_PUBLISH"));
    }

    @Test
    @DisplayName("GET /api/v1/user/profile — wornBadgeKey null when not wearing")
    void getProfile_shouldHaveNullWornBadgeKey() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.wornBadgeKey").value(nullValue()));
    }
}
