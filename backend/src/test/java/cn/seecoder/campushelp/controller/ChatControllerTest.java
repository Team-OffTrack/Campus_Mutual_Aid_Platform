package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.dto.LoginRequest;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private DemandMapper demandMapper;
    @Autowired private ConversationMapper conversationMapper;
    @Autowired private MessageMapper messageMapper;

    private String pubToken;
    private String accToken;
    private String outToken;
    private int openDemandId;
    private int acceptedDemandId;

    @BeforeEach
    void setUp() throws Exception {
        messageMapper.delete(new LambdaQueryWrapper<>());
        conversationMapper.delete(new LambdaQueryWrapper<>());
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

        // Outsider
        RegisterRequest r3 = new RegisterRequest();
        r3.setStudentId("out001"); r3.setPassword("pass123"); r3.setName("Outsider");
        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r3)))
                .andExpect(status().isOk());
        String outBody = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest() {{
                            setStudentId("out001"); setPassword("pass123");
                        }})))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        outToken = objectMapper.readTree(outBody).path("data").path("token").asText();

        // OPEN demand
        String dBody = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "errand", "title", "Open Demand", "description", "test"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        openDemandId = objectMapper.readTree(dBody).path("data").path("demandId").asInt();

        // Accepted demand
        String d2Body = mockMvc.perform(post("/api/v1/demands")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "trade", "title", "Accepted Demand", "description", "test"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        acceptedDemandId = objectMapper.readTree(d2Body).path("data").path("demandId").asInt();
        mockMvc.perform(put("/api/v1/demands/" + acceptedDemandId + "/accept")
                        .header("Authorization", "Bearer " + accToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /chat/conversations creates conversation with publisher")
    void createConversation_withPublisher_shouldReturn200() throws Exception {
        int pubUserId = getPublisherId();

        mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", pubUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").isNumber())
                .andExpect(jsonPath("$.data.otherUserName").value("Publisher"))
                .andExpect(jsonPath("$.data.demandTitle").value("Open Demand"));
    }

    @Test
    @DisplayName("POST /chat/conversations without token returns 403")
    void createConversation_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /chat/conversations returns user's conversations")
    void listConversations_shouldReturnList() throws Exception {
        // First create a conversation
        String rsp = mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", getPublisherId()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int convId = objectMapper.readTree(rsp).path("data").path("conversationId").asInt();

        // List conversations as outsider
        mockMvc.perform(get("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].conversationId").value(convId))
                .andExpect(jsonPath("$.data[0].otherUserName").value("Publisher"));
    }

    @Test
    @DisplayName("POST /chat/conversations/{id}/messages sends a message")
    void sendMessage_shouldReturnMessage() throws Exception {
        String rsp = mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", getPublisherId()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int convId = objectMapper.readTree(rsp).path("data").path("conversationId").asInt();

        mockMvc.perform(post("/api/v1/chat/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Hello!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Hello!"))
                .andExpect(jsonPath("$.data.messageId").isNumber());
    }

    @Test
    @DisplayName("POST /chat/conversations/{id}/messages empty content returns 400")
    void sendMessage_emptyContent_shouldReturn400() throws Exception {
        String rsp = mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", getPublisherId()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int convId = objectMapper.readTree(rsp).path("data").path("conversationId").asInt();

        mockMvc.perform(post("/api/v1/chat/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /chat/conversations/{id}/messages returns messages in order")
    void getMessages_shouldReturnOrdered() throws Exception {
        String rsp = mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", getPublisherId()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int convId = objectMapper.readTree(rsp).path("data").path("conversationId").asInt();

        mockMvc.perform(post("/api/v1/chat/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "First"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/chat/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Second"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/chat/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + outToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].content").value("First"))
                .andExpect(jsonPath("$.data[1].content").value("Second"));
    }

    @Test
    @DisplayName("GET /chat/unread-count returns unread count")
    void unreadCount_shouldReturnCount() throws Exception {
        String rsp = mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", getPublisherId()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int convId = objectMapper.readTree(rsp).path("data").path("conversationId").asInt();

        // Send a message as outsider
        mockMvc.perform(post("/api/v1/chat/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Yo"))))
                .andExpect(status().isOk());

        // Publisher should have 1 unread
        mockMvc.perform(get("/api/v1/chat/unread-count")
                        .header("Authorization", "Bearer " + pubToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("Publisher can chat with acceptor")
    void publisherChatWithAcceptor_shouldReturn200() throws Exception {
        int acceptorUserId = getAcceptorId();

        mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", acceptedDemandId, "targetUserId", acceptorUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.otherUserName").value("Acceptor"));
    }

    @Test
    @DisplayName("Self-chat returns 400")
    void selfChat_shouldReturn400() throws Exception {
        // Publisher messaging themselves — use the publisher's own id
        int pubUserId = getPublisherId();
        mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + pubToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", pubUserId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /chat/conversations/{id}/messages sends an image message")
    void sendImageMessage_shouldReturnMessage() throws Exception {
        String rsp = mockMvc.perform(post("/api/v1/chat/conversations")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("demandId", openDemandId, "targetUserId", getPublisherId()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int convId = objectMapper.readTree(rsp).path("data").path("conversationId").asInt();

        mockMvc.perform(post("/api/v1/chat/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + outToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "image", "imageUrl", "/uploads/chat/test.jpg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageType").value("image"))
                .andExpect(jsonPath("$.data.imageUrl").value("/uploads/chat/test.jpg"))
                .andExpect(jsonPath("$.data.messageId").isNumber());
    }

    @Test
    @DisplayName("POST /chat/upload-image returns 200 with URL")
    void uploadImage_shouldReturnUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image".getBytes());

        mockMvc.perform(multipart("/api/v1/chat/upload-image")
                        .file(file)
                        .header("Authorization", "Bearer " + outToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isString());
    }

    // ── helpers to get real user IDs from tokens ──

    private int getPublisherId() throws Exception {
        String body = mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + pubToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("userId").asInt();
    }

    private int getAcceptorId() throws Exception {
        String body = mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + accToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("userId").asInt();
    }
}
