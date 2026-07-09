package com.examruntime_service.examruntime_service.config.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class MonitorHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        // lay thong tin user security tu attribute ma ben ws authHandShake day vao
        // vi ws la giao thuc rieng nen khong the chay qua springsecurity de xac thuc duoc nen la tu config security rieng o interceptor
        Object principal = attributes.get(WebSocketAuthHandshakeInterceptor.PRINCIPAL_ATTRIBUTE);
        return principal instanceof Principal value ? value : null;
    }
}
