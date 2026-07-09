package com.examruntime_service.examruntime_service.config.websocket;

import com.examruntime_service.examruntime_service.config.security.ExamRuntimeHeaderAuthenticationFilter;
import com.examruntime_service.examruntime_service.config.security.ExamRuntimeUserPrincipal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
/**
 * Chuyen trusted headers tu Gateway thanh WebSocketPrincipal trong luc handshake.
 *
 * REST request dung Security filter binh thuong, con WebSocket upgrade can interceptor rieng.
 * Neu thieu userId/username/role thi reject handshake ngay, tranh tao ket noi anonymous.
 */
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_ATTRIBUTE = "monitorPrincipal";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String userId = firstHeader(request, ExamRuntimeHeaderAuthenticationFilter.USER_ID_HEADER);
        String username = firstHeader(request, ExamRuntimeHeaderAuthenticationFilter.USERNAME_HEADER);
        String role = normalizeRole(firstHeader(request, ExamRuntimeHeaderAuthenticationFilter.USER_ROLE_HEADER));
        if (userId == null || username == null || role == null) {
            // Gateway phai authenticate JWT va forward trusted headers truoc khi request den service.
            return false;
        }
        // put nhung thong tin security vao trong attribute de principal cua ws luu lai.
        attributes.put(PRINCIPAL_ATTRIBUTE,
                new WebSocketPrincipal(new ExamRuntimeUserPrincipal(userId, username, role)));
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String firstHeader(ServerHttpRequest request, String name) {
        List<String> values = request.getHeaders().get(name);
        if (values == null || values.isEmpty() || values.get(0).isBlank()) {
            return null;
        }
        return values.get(0).trim();
    }

    private String normalizeRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String role = value.trim().toUpperCase(Locale.ROOT);
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        return switch (role) {
            case "ADMIN", "TEACHER", "STUDENT" -> "ROLE_" + role;
            default -> null;
        };
    }
}
