package com.examruntime_service.examruntime_service.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
/**
 * Cau hinh STOMP cho luong monitor realtime.
 *
 * Client ket noi vao /v1/api/examruntime-service/ws qua Gateway.
 * Student SEND vao /app/... de backend xu ly.
 * Teacher SUBSCRIBE /topic/... de nhan event ca thi.
 * Student SUBSCRIBE /user/queue/... de nhan alert rieng nhu LOCKED.
 */
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthHandshakeInterceptor authHandshakeInterceptor;
    private final MonitorHandshakeHandler handshakeHandler;
    private final StompAuthorizationInterceptor authorizationInterceptor;

    public WebSocketConfig(
            WebSocketAuthHandshakeInterceptor authHandshakeInterceptor,
            MonitorHandshakeHandler handshakeHandler,
            StompAuthorizationInterceptor authorizationInterceptor
    ) {
        this.authHandshakeInterceptor = authHandshakeInterceptor;
        this.handshakeHandler = handshakeHandler;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint nay van nam duoi prefix public cua gateway de frontend chi can goi Gateway.
        // Handshake handler gan Principal tu trusted headers cho cac frame STOMP ve sau.
        registry.addEndpoint("/v1/api/examruntime-service/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authHandshakeInterceptor)
                .setHandshakeHandler(handshakeHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple broker chi fan-out trong mot instance.
        // Redis Pub/Sub backplane xu ly viec event tu instance A sang teacher dang o instance B.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Chan send/subscribe sai quyen truoc khi message vao controller.
        registration.interceptors(authorizationInterceptor);
    }
}
