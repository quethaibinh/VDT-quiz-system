package com.gateway.gateway.config;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
/**
 * Resolves JWT from the standard Authorization header, with a narrow
 * WebSocket-only fallback for browser handshakes that cannot set that header.
 */
public class JwtTokenResolver {

    public static final String MONITOR_WS_PATH = "/v1/api/examruntime-service/ws";
    private static final String BEARER_PREFIX = "Bearer ";

    public String resolve(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        if (isMonitorWebSocketPath(exchange)) {
            String token = exchange.getRequest().getQueryParams().getFirst("access_token");
            if (token != null && !token.isBlank()) {
                return token.trim();
            }
        }
        return null;
    }

    private boolean isMonitorWebSocketPath(ServerWebExchange exchange) {
        return MONITOR_WS_PATH.equals(exchange.getRequest().getURI().getPath());
    }
}
