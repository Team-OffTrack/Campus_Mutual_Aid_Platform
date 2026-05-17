package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.LoginRequest;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.EvaluationMapper;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private DemandMapper demandMapper;
    @Autowired private EvaluationMapper evaluationMapper;

    private String pubToken;
    private String accToken;
    private int completedDemandId;

    @BeforeEach
    void setUp() throws Exception {
        evaluationMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Publisher
        RegisterRequest r1 = new RegisterRequest();
        r1.setStudentId("pub001"); r1.setPassword("pass123"); r1.setName("Publisher");
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r1)))
                .andExpect(status().isOk());
        String pubBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest() {{
                            setStudentId("pub001"); setPassword("pass123");
                        }})))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        pubToken = objectMapper.readTree(pubBody).path("data").path("token").asText();

        // Acceptor
        RegisterRequest r2 = new RegisterRequest();
        r2.setStudentId("acc001"); r2.setPassword("pass123"); r2.setName("Acceptor");
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r2)))
                .andExpect(status().isOk());
        String accBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest() {{
                            setStudentId("acc001"); setPassword("pass123");
                        }})))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        accToken = objectMapper.readTree(accBody).path("data").path("token").asText();

        // Publish, accept, complete
        String dBody = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "errand", "title", "Eval Test", "description", "test"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        completedDemandId = objectMapper.readTree(dBody).path("data").path("demandId").asInt();

        mockMvc.perform(put("/api/v1/demands/" + completedDemandId + "/accept")
                        .header("Authorization", "Bearer " + accToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/demands/" + completedDemandId + "/complete")
                        .header("Authorization", "Bearer " + pubToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /evaluations creates evaluation")
    void create_shouldReturnEvaluation() throws Exception {
        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 4, "comment", "Good"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.evaluatorName").value("Publisher"))
                .andExpect(jsonPath("$.data.evaluatorId").isNumber());
    }

    @Test
    @DisplayName("POST /evaluations duplicate returns 409")
    void create_duplicate_shouldReturn409() throws Exception {
        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 4))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("POST /evaluations non-participant returns 403")
    void create_nonParticipant_shouldReturn403() throws Exception {
        // Register third user
        RegisterRequest r3 = new RegisterRequest();
        r3.setStudentId("outsider"); r3.setPassword("pass123"); r3.setName("Outsider");
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r3)))
                .andExpect(status().isOk());
        String outBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest() {{
                            setStudentId("outsider"); setPassword("pass123");
                        }})))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String outToken = objectMapper.readTree(outBody).path("data").path("token").asText();

        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 3))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("POST /evaluations with invalid rating returns 400")
    void create_invalidRating_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 0))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 6))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /evaluations/demand/{id} returns evaluations for demand")
    void getByDemand_shouldReturnList() throws Exception {
        // Both rate
        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 4))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + accToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 5))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/evaluations/demand/" + completedDemandId)
                        .header("Authorization", "Bearer " + pubToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /evaluations/mine returns my evaluation or null")
    void getMine_shouldReturnMyEval() throws Exception {
        // Not rated yet — should return null body
        mockMvc.perform(get("/api/v1/evaluations/mine")
                        .header("Authorization", "Bearer " + pubToken)
                        .param("demandId", String.valueOf(completedDemandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // Rate
        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 4))))
                .andExpect(status().isOk());

        // Now should return it
        mockMvc.perform(get("/api/v1/evaluations/mine")
                        .header("Authorization", "Bearer " + pubToken)
                        .param("demandId", String.valueOf(completedDemandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.evaluatorId").isNumber());
    }

    @Test
    @DisplayName("GET /evaluations/user/{id} returns received evaluations")
    void getByUser_shouldReturnReceived() throws Exception {
        String rsp = mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 5))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int targetId = objectMapper.readTree(rsp).path("data").path("targetUserId").asInt();

        mockMvc.perform(get("/api/v1/evaluations/user/" + targetId)
                        .header("Authorization", "Bearer " + pubToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].rating").value(5));
    }

    @Test
    @DisplayName("PUT /evaluations/{id} updates rating")
    void update_shouldModifyRating() throws Exception {
        String body = mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 3))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int evalId = objectMapper.readTree(body).path("data").path("evaluationId").asInt();

        mockMvc.perform(put("/api/v1/evaluations/" + evalId)
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 5, "comment", "much better"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("much better"));
    }

    @Test
    @DisplayName("PUT /evaluations/{id} by non-owner returns 403")
    void update_byNonOwner_shouldReturn403() throws Exception {
        String body = mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 3))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int evalId = objectMapper.readTree(body).path("data").path("evaluationId").asInt();

        mockMvc.perform(put("/api/v1/evaluations/" + evalId)
                        .header("Authorization", "Bearer " + accToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("POST /evaluations without token returns 403")
    void create_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 3))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Both participants can evaluate each other independently")
    void bidirectional_shouldWork() throws Exception {
        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 4))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/evaluations")
                        .header("Authorization", "Bearer " + accToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", completedDemandId, "rating", 5))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/evaluations/demand/" + completedDemandId)
                        .header("Authorization", "Bearer " + pubToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

}
