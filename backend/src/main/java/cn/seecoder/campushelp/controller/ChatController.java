package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.ConversationResponse;
import cn.seecoder.campushelp.dto.CreateConversationRequest;
import cn.seecoder.campushelp.dto.SendMessageRequest;
import cn.seecoder.campushelp.entity.Message;
import cn.seecoder.campushelp.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Private chat endpoints scoped to demands.
 * <p>
 * Conversations link two users (e.g. visitor↔publisher, publisher↔acceptor)
 * and all messages are exchanged within a conversation.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Create or return an existing conversation between the current user and a target user about a demand. */
    @PostMapping("/conversations")
    public ApiResult<ConversationResponse> createConversation(Authentication auth,
                                                              @Valid @RequestBody CreateConversationRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(chatService.getOrCreateConversation(userId, req.getDemandId(), req.getTargetUserId()));
    }

    /** List all conversations for the current user, newest first. */
    @GetMapping("/conversations")
    public ApiResult<List<ConversationResponse>> listConversations(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(chatService.listConversations(userId));
    }

    /** Get all messages in a conversation, oldest first. Marks unread messages from the other party as read. */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResult<List<Message>> getMessages(Authentication auth,
                                                 @PathVariable Long conversationId) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(chatService.getMessages(conversationId, userId));
    }

    /** Send a message (text or image) in a conversation. */
    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResult<Message> sendMessage(Authentication auth,
                                           @PathVariable Long conversationId,
                                           @Valid @RequestBody SendMessageRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(chatService.sendMessage(
                conversationId, userId, req.getType(), req.getContent(), req.getImageUrl()));
    }

    /** Upload a chat image. Returns the server-relative URL to reference in sendMessage. */
    @PostMapping("/upload-image")
    public ApiResult<String> uploadImage(Authentication auth,
                                         @RequestParam("file") MultipartFile file) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(chatService.uploadImage(userId, file));
    }

    /** Total unread chat messages for the current user (for badge display). */
    @GetMapping("/unread-count")
    public ApiResult<Map<String, Long>> unreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(Map.of("count", chatService.unreadCount(userId)));
    }
}
