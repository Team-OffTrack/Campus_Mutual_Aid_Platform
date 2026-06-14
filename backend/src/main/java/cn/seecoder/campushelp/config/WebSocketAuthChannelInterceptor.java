package cn.seecoder.campushelp.config;

import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Authenticates STOMP CONNECT frames by validating the JWT token
 * from the {@code Authorization} header, mirroring the HTTP security
 * in {@link cn.seecoder.campushelp.security.JwtAuthenticationFilter}.
 * <p>
 * On successful authentication the user principal is attached to the
 * STOMP session so {@code SimpMessagingTemplate.convertAndSendToUser}
 * can route to the correct destination.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public WebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, UserMapper userMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            // For non-CONNECT frames, verify the user is already authenticated
            if (accessor != null && accessor.getCommand() != null && accessor.getUser() == null) {
                throw new BadCredentialsException("Not authenticated");
            }
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT rejected: missing or malformed Authorization header");
            throw new BadCredentialsException("Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket CONNECT rejected: invalid or expired JWT");
            throw new BadCredentialsException("Invalid or expired JWT token");
        }

        Claims claims = jwtTokenProvider.parseClaims(token);
        Long userId = Long.valueOf(claims.getSubject());

        // Reject tokens belonging to banned or deleted users
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            log.warn("WebSocket CONNECT rejected: user {} not found or banned", userId);
            throw new BadCredentialsException("User not found or banned");
        }

        String role = claims.get("role", String.class);
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, token, authorities);
        accessor.setUser(authentication);

        log.debug("WebSocket CONNECT authenticated: userId={}", userId);
        return message;
    }
}
