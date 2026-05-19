package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.ConversationResponse;
import cn.seecoder.campushelp.entity.*;
import cn.seecoder.campushelp.mapper.*;
import cn.seecoder.campushelp.service.ChatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Manages private chat conversations and messages.
 * <p>
 * Conversations are always scoped to a demand. The valid participant pairs are:
 * <ul>
 *   <li>Anyone ↔ demand publisher (visitor messages publisher)</li>
 *   <li>Publisher ↔ acceptor (publisher messages the person who accepted)</li>
 * </ul>
 * user1_id always holds the smaller user id so the UNIQUE constraint works deterministically.
 */
@Service
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;

    public ChatServiceImpl(ConversationMapper conversationMapper,
                           MessageMapper messageMapper,
                           DemandMapper demandMapper,
                           UserMapper userMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(Long userId, Long demandId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能和自己私聊");
        }

        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        }

        // Validate chat permission: target must be publisher OR (user is publisher AND target is acceptor)
        boolean targetIsPublisher = targetUserId.equals(demand.getPublisherId());
        boolean userIsPublisherChattingWithAcceptor = userId.equals(demand.getPublisherId())
                && targetUserId.equals(demand.getAcceptorId());

        if (!targetIsPublisher && !userIsPublisherChattingWithAcceptor) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无法与该用户私聊");
        }

        // Ensure deterministic (user1, user2) ordering for UNIQUE constraint
        long u1 = Math.min(userId, targetUserId);
        long u2 = Math.max(userId, targetUserId);

        // Return existing conversation if present
        Conversation existing = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getDemandId, demandId)
                        .eq(Conversation::getUser1Id, u1)
                        .eq(Conversation::getUser2Id, u2));
        if (existing != null) {
            return buildResponse(existing, userId);
        }

        Conversation conv = new Conversation();
        conv.setDemandId(demandId);
        conv.setUser1Id(u1);
        conv.setUser2Id(u2);
        try {
            conversationMapper.insert(conv);
        } catch (DuplicateKeyException e) {
            Conversation reQueried = conversationMapper.selectOne(
                    new LambdaQueryWrapper<Conversation>()
                            .eq(Conversation::getDemandId, demandId)
                            .eq(Conversation::getUser1Id, u1)
                            .eq(Conversation::getUser2Id, u2));
            return buildResponse(reQueried, userId);
        }

        return buildResponse(conv, userId);
    }

    @Override
    public List<ConversationResponse> listConversations(Long userId) {
        List<Conversation> convs = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .and(w -> w.eq(Conversation::getUser1Id, userId)
                                .or().eq(Conversation::getUser2Id, userId))
                        .orderByDesc(Conversation::getLastMessageAt)
                        .orderByDesc(Conversation::getCreateTime));

        List<ConversationResponse> result = new ArrayList<>();
        for (Conversation c : convs) {
            result.add(buildResponse(c, userId));
        }
        return result;
    }

    @Override
    @Transactional
    public List<Message> getMessages(Long conversationId, Long userId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对话不存在");
        }
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该对话");
        }

        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreateTime));

        // Mark messages from the other party as read
        for (Message m : messages) {
            if (!m.getSenderId().equals(userId) && !m.isRead()) {
                m.setIsRead(1);
                messageMapper.updateById(m);
            }
        }

        return messages;
    }

    @Override
    @Transactional
    public Message sendMessage(Long conversationId, Long userId, String type, String content, String imageUrl) {
        String msgType = type != null && !type.isBlank() ? type.trim() : "text";

        if ("image".equals(msgType)) {
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "图片消息需要上传图片");
            }
        } else {
            String trimmed = content != null ? content.trim() : "";
            if (trimmed.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "消息不能为空");
            }
        }

        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对话不存在");
        }
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权在此对话发送消息");
        }

        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(userId);
        msg.setMessageType(msgType);
        msg.setContent(content != null ? content.trim() : "");
        msg.setImageUrl("image".equals(msgType) ? imageUrl : null);
        msg.setIsRead(0);
        messageMapper.insert(msg);

        // Update conversation snapshot
        String snapshot = "image".equals(msgType) ? "[图片]" : msg.getContent();
        conv.setLastMessage(snapshot.length() > 500 ? snapshot.substring(0, 500) : snapshot);
        conv.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conv);

        return msg;
    }

    @Override
    public String uploadImage(Long userId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只支持图片文件");
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || ext.isBlank()) ext = "jpg";
        String filename = userId + "_" + System.currentTimeMillis() + "." + ext.toLowerCase();

        try {
            Path dir = Path.of("uploads", "chat");
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toAbsolutePath());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片上传失败");
        }

        return "/uploads/chat/" + filename;
    }

    @Override
    public long unreadCount(Long userId) {
        List<Conversation> convs = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .select(Conversation::getConversationId)
                        .and(w -> w.eq(Conversation::getUser1Id, userId)
                                .or().eq(Conversation::getUser2Id, userId)));
        if (convs.isEmpty()) return 0;

        List<Long> convIds = convs.stream().map(Conversation::getConversationId).toList();
        return messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .in(Message::getConversationId, convIds)
                        .ne(Message::getSenderId, userId)
                        .eq(Message::getIsRead, 0));
    }

    // ── private helpers ──

    private ConversationResponse buildResponse(Conversation c, Long userId) {
        ConversationResponse r = new ConversationResponse();
        r.setConversationId(c.getConversationId());
        r.setDemandId(c.getDemandId());
        r.setLastMessage(c.getLastMessage());
        r.setLastMessageAt(c.getLastMessageAt());

        Long otherId = c.otherUserId(userId);
        r.setOtherUserId(otherId);

        // Load other user name and avatar
        User other = userMapper.selectById(otherId);
        r.setOtherUserName(other != null ? other.getName() : "未知用户");
        r.setOtherUserAvatar(other != null ? other.getAvatar() : null);

        // Load demand title
        Demand demand = demandMapper.selectById(c.getDemandId());
        r.setDemandTitle(demand != null ? demand.getTitle() : "—");

        // Count unread messages from the other party
        long unread = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, c.getConversationId())
                        .eq(Message::getSenderId, otherId)
                        .eq(Message::getIsRead, 0));
        r.setUnreadCount(unread);

        return r;
    }
}
