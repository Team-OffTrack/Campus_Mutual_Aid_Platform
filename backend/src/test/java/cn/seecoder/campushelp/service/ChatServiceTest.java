package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.entity.*;
import cn.seecoder.campushelp.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired private ChatService chatService;
    @Autowired private DemandService demandService;
    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private DemandMapper demandMapper;
    @Autowired private ConversationMapper conversationMapper;
    @Autowired private MessageMapper messageMapper;

    private Long publisherId;
    private Long acceptorId;
    private Long outsiderId;
    private Long openDemandId;
    private Long acceptedDemandId;

    @BeforeEach
    void setUp() {
        messageMapper.delete(new LambdaQueryWrapper<>());
        conversationMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Publisher
        RegisterRequest r1 = new RegisterRequest();
        r1.setStudentId("pub001"); r1.setPassword("pass123"); r1.setName("Publisher");
        userService.register(r1);
        publisherId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "pub001")).getUserId();

        // Acceptor
        RegisterRequest r2 = new RegisterRequest();
        r2.setStudentId("acc001"); r2.setPassword("pass123"); r2.setName("Acceptor");
        userService.register(r2);
        acceptorId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "acc001")).getUserId();

        // Outsider
        RegisterRequest r3 = new RegisterRequest();
        r3.setStudentId("out001"); r3.setPassword("pass123"); r3.setName("Outsider");
        userService.register(r3);
        outsiderId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "out001")).getUserId();

        // OPEN demand
        CreateDemandRequest dReq = new CreateDemandRequest();
        dReq.setType("errand"); dReq.setTitle("Open Demand"); dReq.setDescription("test");
        DemandResponse created = demandService.publish(publisherId, dReq);
        openDemandId = created.getDemandId();

        // IN_PROGRESS demand (accepted)
        CreateDemandRequest d2 = new CreateDemandRequest();
        d2.setType("trade"); d2.setTitle("Accepted Demand"); d2.setDescription("test");
        DemandResponse created2 = demandService.publish(publisherId, d2);
        acceptedDemandId = created2.getDemandId();
        demandService.accept(acceptedDemandId, acceptorId);
    }

    @Test
    @DisplayName("Anyone can start a conversation with the demand publisher")
    void anyoneCanChatWithPublisher_shouldCreateConversation() {
        ConversationResponse rsp = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        assertNotNull(rsp.getConversationId());
        assertEquals(publisherId, rsp.getOtherUserId());
        assertEquals("Publisher", rsp.getOtherUserName());
        assertEquals("Open Demand", rsp.getDemandTitle());
        assertEquals(0, rsp.getUnreadCount());
    }

    @Test
    @DisplayName("Acceptor can chat with publisher")
    void acceptorCanChatWithPublisher_shouldCreateConversation() {
        ConversationResponse rsp = chatService.getOrCreateConversation(acceptorId, acceptedDemandId, publisherId);
        assertNotNull(rsp.getConversationId());
        assertEquals(publisherId, rsp.getOtherUserId());
    }

    @Test
    @DisplayName("Publisher can chat with acceptor")
    void publisherCanChatWithAcceptor_shouldCreateConversation() {
        ConversationResponse rsp = chatService.getOrCreateConversation(publisherId, acceptedDemandId, acceptorId);
        assertNotNull(rsp.getConversationId());
        assertEquals(acceptorId, rsp.getOtherUserId());
        assertEquals("Acceptor", rsp.getOtherUserName());
    }

    @Test
    @DisplayName("Self-chat is rejected")
    void selfChat_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.getOrCreateConversation(publisherId, openDemandId, publisherId));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("自己"));
    }

    @Test
    @DisplayName("Chat with non-participant non-publisher is rejected")
    void chatWithRandomUser_shouldThrow() {
        // Outsider tries to chat with acceptor (who is neither publisher nor a publisher chatting with acceptor)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.getOrCreateConversation(outsiderId, acceptedDemandId, acceptorId));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("Duplicate getOrCreate returns existing conversation")
    void duplicateGetOrCreate_shouldReturnExisting() {
        ConversationResponse r1 = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        ConversationResponse r2 = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        assertEquals(r1.getConversationId(), r2.getConversationId());
    }

    @Test
    @DisplayName("Conversation participant ordering is deterministic")
    void participantOrdering_shouldBeDeterministic() {
        chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        // Only one conversation should exist for this pair
        long count = conversationMapper.selectCount(new LambdaQueryWrapper<>());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Send message creates a message and updates conversation snapshot")
    void sendMessage_shouldPersistAndUpdateSnapshot() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);

        Message msg = chatService.sendMessage(conv.getConversationId(), outsiderId, "text", "Hello publisher!", null);
        assertNotNull(msg.getMessageId());
        assertEquals("Hello publisher!", msg.getContent());
        assertEquals(outsiderId, msg.getSenderId());
        assertEquals(conv.getConversationId(), msg.getConversationId());

        // Verify conversation snapshot was updated
        Conversation c = conversationMapper.selectById(conv.getConversationId());
        assertEquals("Hello publisher!", c.getLastMessage());
        assertNotNull(c.getLastMessageAt());
    }

    @Test
    @DisplayName("Send message trims content")
    void sendMessage_shouldTrimContent() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        Message msg = chatService.sendMessage(conv.getConversationId(), outsiderId, "text", "  trimmed  ", null);
        assertEquals("trimmed", msg.getContent());
    }

    @Test
    @DisplayName("Send message to non-participant conversation throws FORBIDDEN")
    void sendMessage_nonParticipant_shouldThrow() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        // Acceptor is not part of this conversation
        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.sendMessage(conv.getConversationId(), acceptorId, "text", "Hi", null));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("Send message with blank content throws BAD_REQUEST")
    void sendMessage_blankContent_shouldThrow() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        assertThrows(BusinessException.class,
                () -> chatService.sendMessage(conv.getConversationId(), outsiderId, "text", "   ", null));
        assertThrows(BusinessException.class,
                () -> chatService.sendMessage(conv.getConversationId(), outsiderId, "text", "", null));
    }

    @Test
    @DisplayName("Get messages returns in chronological order and marks read")
    void getMessages_shouldReturnOrderedAndMarkRead() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);

        chatService.sendMessage(conv.getConversationId(), outsiderId, "text", "First", null);
        chatService.sendMessage(conv.getConversationId(), publisherId, "text", "Second", null);
        chatService.sendMessage(conv.getConversationId(), outsiderId, "text", "Third", null);

        // Get from publisher's view
        List<Message> msgs = chatService.getMessages(conv.getConversationId(), publisherId);
        assertEquals(3, msgs.size());
        assertEquals("First", msgs.get(0).getContent());
        assertEquals("Second", msgs.get(1).getContent());
        assertEquals("Third", msgs.get(2).getContent());

        // Messages from outsider (not publisher) should now be marked read
        for (Message m : msgs) {
            if (!m.getSenderId().equals(publisherId)) {
                assertTrue(m.isRead());
            }
        }
    }

    @Test
    @DisplayName("Get messages for non-participant throws FORBIDDEN")
    void getMessages_nonParticipant_shouldThrow() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.getMessages(conv.getConversationId(), acceptorId));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("Get messages on non-existent conversation throws NOT_FOUND")
    void getMessages_nonExistent_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.getMessages(9999L, outsiderId));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("List conversations shows all user's conversations with unread counts")
    void listConversations_shouldReturnWithUnreadCounts() {
        // Outsider chats with publisher about openDemand
        ConversationResponse conv1 = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        chatService.sendMessage(conv1.getConversationId(), outsiderId, "text", "Hi", null);

        // Publisher chats with acceptor about acceptedDemand
        ConversationResponse conv2 = chatService.getOrCreateConversation(publisherId, acceptedDemandId, acceptorId);
        chatService.sendMessage(conv2.getConversationId(), publisherId, "text", "Ready?", null);

        // Publisher lists conversations
        List<ConversationResponse> pubConvs = chatService.listConversations(publisherId);
        assertEquals(2, pubConvs.size());

        // The conversation where outsider sent a message should have unread=1 for publisher
        ConversationResponse fromOutsider = pubConvs.stream()
                .filter(c -> c.getOtherUserId().equals(outsiderId)).findFirst().orElseThrow();
        assertEquals(1, fromOutsider.getUnreadCount());

        // The conversation where publisher sent a message should have unread=0 for publisher
        ConversationResponse fromAcceptor = pubConvs.stream()
                .filter(c -> c.getOtherUserId().equals(acceptorId)).findFirst().orElseThrow();
        assertEquals(0, fromAcceptor.getUnreadCount());
    }

    @Test
    @DisplayName("Unread count aggregates across all conversations")
    void unreadCount_shouldAggregate() {
        ConversationResponse conv1 = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        chatService.sendMessage(conv1.getConversationId(), outsiderId, "text", "Msg 1", null);
        chatService.sendMessage(conv1.getConversationId(), outsiderId, "text", "Msg 2", null);

        ConversationResponse conv2 = chatService.getOrCreateConversation(acceptorId, acceptedDemandId, publisherId);
        chatService.sendMessage(conv2.getConversationId(), acceptorId, "text", "Msg 3", null);

        // Publisher has 3 unread (2 from outsider + 1 from acceptor)
        assertEquals(3, chatService.unreadCount(publisherId));
        // Outsider has 0 unread (they sent all messages)
        assertEquals(0, chatService.unreadCount(outsiderId));
    }

    @Test
    @DisplayName("Unread count decreases after reading messages")
    void unreadCount_decreasesAfterReading() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        chatService.sendMessage(conv.getConversationId(), outsiderId, "text", "Hello", null);
        assertEquals(1, chatService.unreadCount(publisherId));

        // Publisher reads messages → marks outsider's message as read
        chatService.getMessages(conv.getConversationId(), publisherId);
        assertEquals(0, chatService.unreadCount(publisherId));
    }

    @Test
    @DisplayName("Send image message stores message type and imageUrl")
    void sendImageMessage_shouldStoreTypeAndUrl() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        Message msg = chatService.sendMessage(conv.getConversationId(), outsiderId,
                "image", "", "/uploads/chat/test.jpg");
        assertNotNull(msg.getMessageId());
        assertEquals("image", msg.getMessageType());
        assertEquals("/uploads/chat/test.jpg", msg.getImageUrl());
        assertTrue(msg.isImage());

        // Verify conversation snapshot shows [图片]
        Conversation c = conversationMapper.selectById(conv.getConversationId());
        assertEquals("[图片]", c.getLastMessage());
    }

    @Test
    @DisplayName("Send image message without imageUrl throws BAD_REQUEST")
    void sendImageMessage_withoutUrl_shouldThrow() {
        ConversationResponse conv = chatService.getOrCreateConversation(outsiderId, openDemandId, publisherId);
        assertThrows(BusinessException.class,
                () -> chatService.sendMessage(conv.getConversationId(), outsiderId, "image", "", null));
        assertThrows(BusinessException.class,
                () -> chatService.sendMessage(conv.getConversationId(), outsiderId, "image", "", "  "));
    }
}
