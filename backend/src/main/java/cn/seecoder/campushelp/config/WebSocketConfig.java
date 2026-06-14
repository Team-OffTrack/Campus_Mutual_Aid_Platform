package cn.seecoder.campushelp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration.
 * <p>
 * Provides real-time push for chat messages and notifications
 * via a simple in-memory broker. Clients connect at {@code /ws}
 * with SockJS fallback and authenticate by passing the JWT
 * as a STOMP {@code Authorization} header.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authInterceptor;

    public WebSocketConfig(WebSocketAuthChannelInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory broker for per-user queues and broadcast topics
        config.enableSimpleBroker("/queue", "/topic");
        // Client sends messages to /app/... (reserved for future @MessageMapping controllers)
        config.setApplicationDestinationPrefixes("/app");
        // /user prefix resolves to per-session user queues (e.g. /user/42/queue/chat)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback endpoint (for browsers that lack native WebSocket)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // Native WebSocket endpoint (no SockJS — used by modern frontend clients)
        registry.addEndpoint("/ws-raw")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
