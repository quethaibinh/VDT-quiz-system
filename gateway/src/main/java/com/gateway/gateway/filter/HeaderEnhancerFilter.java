package com.gateway.gateway.filter;

import com.gateway.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
/**
 * Thay cac identity header cua client bang du lieu da xac thuc tu JWT.
 */
public class HeaderEnhancerFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    public HeaderEnhancerFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Luon xoa identity header cua client de ngan gia mao nguoi dung.
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-Username");
                })
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.getClaims(token);
                String userId = claims.get("userId", String.class);
                String userRole = claims.get("userRole", String.class);
                String username = claims.get("username", String.class);

                // Service phia sau chi nhan header dinh danh duoc tao tu JWT hop le.
                ServerHttpRequest trustedRequest = sanitizedRequest.mutate()
                        .headers(headers -> {
                            putIfPresent(headers, "X-User-Id", userId);
                            putIfPresent(headers, "X-User-Role", userRole);
                            putIfPresent(headers, "X-Username", username);
                        })
                        .build();

                return chain.filter(exchange.mutate().request(trustedRequest).build());
            } catch (Exception ignored) {
                return chain.filter(sanitizedExchange);
            }
        }

        return chain.filter(sanitizedExchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private void putIfPresent(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }
}
