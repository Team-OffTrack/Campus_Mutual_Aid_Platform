package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.ConversationResponse;
import cn.seecoder.campushelp.entity.Message;
import java.util.List;

/**
 * Manages private conversations and messages scoped to demands.
 * <p>
 * A conversation links two users within a demand context.
 * Either party can send text messages once the conversation exists.
 * At least one participant must be the demand publisher.
 */
public interface ChatService {

    /**
     * Find or create a conversation between the current user and a target user
     * about a specific demand. Validates that the chat is allowed:
     * the target must be the demand publisher, or the current user is the publisher
     * and the target is the acceptor.
     */
    ConversationResponse getOrCreateConversation(Long userId, Long demandId, Long targetUserId);

    /** List all conversations for a user, newest first, with participant info and unread counts. */
    List<ConversationResponse> listConversations(Long userId);

    /**
     * Get all messages in a conversation, oldest first.
     * Marks messages from the other party as read.
     */
    List<Message> getMessages(Long conversationId, Long userId);

    /** Send a text message in an existing conversation. Updates the conversation's last-message snapshot. */
    Message sendMessage(Long conversationId, Long userId, String content);

    /** Count total unread chat messages for a user across all conversations. */
    long unreadCount(Long userId);
}
