package cn.seecoder.campushelp.config;

import cn.seecoder.campushelp.dto.ConversationResponse;
import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.entity.Message;
import cn.seecoder.campushelp.entity.Notification;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.*;
import cn.seecoder.campushelp.security.JwtTokenProvider;
import cn.seecoder.campushelp.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end WebSocket integration tests using a real STOMP client.
 * <p>
 * Verifies JWT authentication on CONNECT and real-time push
 * for chat messages and notifications.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserService userService;
    @Autowired private DemandService demandService;
    @Autowired private ChatService chatService;
    @Autowired private NotificationService notificationService;
    @Autowired private UserMapper userMapper;
    @Autowired private DemandMapper demandMapper;
    @Autowired private ConversationMapper conversationMapper;
    @Autowired private MessageMapper messageMapper;
    @Autowired private NotificationMapper notificationMapper;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        messageMapper.delete(new LambdaQueryWrapper<>());
        conversationMapper.delete(new LambdaQueryWrapper<>());
        notificationMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        Transport webSocketTransport = new WebSocketTransport(new StandardWebSocketClient());
        SockJsClient sockJsClient = new SockJsClient(List.of(webSocketTransport));
        stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        stompClient.setMessageConverter(converter);
    }

    // ── helpers ──

    private String registerAndGetToken(String studentId, String name) {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId(studentId);
        req.setPassword("pass123");
        req.setName(name);
        userService.register(req);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, studentId));
        return jwtTokenProvider.generateToken(user.getUserId(), user.getStudentId(), user.getRole());
    }

    private User getUser(String studentId) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, studentId));
    }

    private StompSession connect(String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        return stompClient.connectAsync(
                "http://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }

    // ── tests ──

    @Test
    @DisplayName("STOMP CONNECT with valid JWT authenticates successfully")
    void connectWithValidJwt_shouldAuthenticate() throws Exception {
        String token = registerAndGetToken("ws001", "WebSocket用户");

        StompSession session = connect(token);
        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    @DisplayName("STOMP CONNECT with invalid JWT is rejected")
    void connectWithInvalidJwt_shouldBeRejected() {
        assertThrows(ExecutionException.class, () -> connect("invalid-token"));
    }

    @Test
    @DisplayName("STOMP CONNECT with empty Authorization header is rejected")
    void connectWithoutAuth_shouldBeRejected() {
        assertThrows(ExecutionException.class, () -> {
            stompClient.connectAsync(
                    "http://localhost:" + port + "/ws",
                    new WebSocketHttpHeaders(),
                    new StompHeaders(), // no Authorization
                    new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        });
    }

    @Test
    @DisplayName("Chat message pushed to recipient via WebSocket /user/queue/chat")
    void chatMessage_shouldPushToRecipient() throws Exception {
        // Setup: publisher and outsider
        String pubToken = registerAndGetToken("pub002", "发布者");
        String outToken = registerAndGetToken("out002", "访客");
        User publisher = getUser("pub002");
        User outsider = getUser("out002");

        // Publish demand
        CreateDemandRequest dReq = new CreateDemandRequest();
        dReq.setType("errand"); dReq.setTitle("实时聊天测试"); dReq.setDescription("test");
        DemandResponse created = demandService.publish(publisher.getUserId(), dReq);

        // Create conversation between outsider and publisher
        ConversationResponse conv = chatService.getOrCreateConversation(
                outsider.getUserId(), created.getDemandId(), publisher.getUserId());

        // Outsider connects via STOMP and subscribes to chat queue
        StompSession outSession = connect(outToken);
        BlockingQueue<Message> received = new LinkedBlockingQueue<>();
        StompHeaders subHeaders = new StompHeaders();
        subHeaders.setDestination("/user/queue/chat");
        outSession.subscribe(subHeaders, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Message.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Message) payload);
            }
        });

        // Allow subscription to propagate before sending
        Thread.sleep(200);

        // Publisher sends message via service (simulating REST call)
        Message msg = chatService.sendMessage(conv.getConversationId(), publisher.getUserId(),
                "text", "Hello via WebSocket!", null);

        // Outsider should receive the push
        Message pushed = received.poll(10, TimeUnit.SECONDS);
        assertNotNull(pushed, "Recipient should receive chat message via WebSocket");
        assertEquals("Hello via WebSocket!", pushed.getContent());
        assertEquals(publisher.getUserId(), pushed.getSenderId());
        assertEquals(msg.getMessageId(), pushed.getMessageId());

        outSession.disconnect();
    }

    @Test
    @DisplayName("Notification pushed to user via WebSocket /user/queue/notifications")
    void notification_shouldPushToUser() throws Exception {
        String token = registerAndGetToken("notif002", "通知接收者");
        User user = getUser("notif002");

        // Connect via STOMP and subscribe to notification queue
        StompSession session = connect(token);
        BlockingQueue<Notification> received = new LinkedBlockingQueue<>();
        StompHeaders subHeaders = new StompHeaders();
        subHeaders.setDestination("/user/queue/notifications");
        session.subscribe(subHeaders, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Notification.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Notification) payload);
            }
        });

        // Allow subscription to propagate before triggering
        Thread.sleep(200);

        // Trigger notification creation
        Notification created = notificationService.create(user.getUserId(), "ACCEPT",
                "有人接单了", "您的需求已被张三接单", null);

        // User should receive the push
        Notification pushed = received.poll(10, TimeUnit.SECONDS);
        assertNotNull(pushed, "User should receive notification via WebSocket");
        assertEquals("有人接单了", pushed.getTitle());
        assertEquals(created.getNotificationId(), pushed.getNotificationId());

        session.disconnect();
    }

    @Test
    @DisplayName("Offline recipient does not cause errors (message still persisted)")
    void offlineRecipient_shouldNotThrow() {
        // Setup sender, create conversation, send message — no STOMP connection from recipient
        String pubToken = registerAndGetToken("pub003", "发送者离线测试");
        String outToken = registerAndGetToken("out003", "离线接收者");
        User publisher = getUser("pub003");
        User outsider = getUser("out003");

        CreateDemandRequest dReq = new CreateDemandRequest();
        dReq.setType("errand"); dReq.setTitle("离线消息测试"); dReq.setDescription("test");
        DemandResponse created = demandService.publish(publisher.getUserId(), dReq);

        ConversationResponse conv = chatService.getOrCreateConversation(
                outsider.getUserId(), created.getDemandId(), publisher.getUserId());

        // Outsider does NOT connect — sendMessage should still succeed
        Message msg = chatService.sendMessage(conv.getConversationId(), publisher.getUserId(),
                "text", "Message to offline user", null);
        assertNotNull(msg.getMessageId());

        // Message is persisted even though recipient is offline
        Message persisted = messageMapper.selectById(msg.getMessageId());
        assertEquals("Message to offline user", persisted.getContent());
    }
}
