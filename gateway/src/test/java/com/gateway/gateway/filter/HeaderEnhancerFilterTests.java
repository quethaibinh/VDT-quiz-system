package com.gateway.gateway.filter;

import com.gateway.gateway.util.JwtUtil;
import com.gateway.gateway.config.JwtTokenResolver;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeaderEnhancerFilterTests {

    @Test
    void removesSpoofedUserHeadersWhenRequestHasNoToken() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        HeaderEnhancerFilter filter = new HeaderEnhancerFilter(jwtUtil, new JwtTokenResolver(), "test-gateway-secret");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/public")
                        .header("X-User-Id", "fake-user")
                        .header("X-User-Role", "ADMIN")
                        .header("X-Username", "fake-admin")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = currentExchange -> {
            forwardedExchange.set(currentExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedExchange.get()).isNotNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Role")).isNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Username")).isNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Gateway-Secret")).isNull();
    }

    @Test
    void replacesSpoofedHeadersWithClaimsFromValidToken() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(jwtUtil.getClaims("valid-token")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("trusted-user");
        when(claims.get("userRole", String.class)).thenReturn("STUDENT");
        when(claims.get("username", String.class)).thenReturn("student01");

        HeaderEnhancerFilter filter = new HeaderEnhancerFilter(jwtUtil, new JwtTokenResolver(), "test-gateway-secret");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/protected")
                        .header("Authorization", "Bearer valid-token")
                        .header("X-User-Id", "fake-user")
                        .header("X-User-Role", "ADMIN")
                        .header("X-Username", "fake-admin")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = currentExchange -> {
            forwardedExchange.set(currentExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("trusted-user");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("STUDENT");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Username"))
                .isEqualTo("student01");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Gateway-Secret"))
                .isEqualTo("test-gateway-secret");
    }

    @Test
    void acceptsAccessTokenOnlyForMonitorWebSocketPath() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(jwtUtil.getClaims("ws-token")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn("teacher-id");
        when(claims.get("userRole", String.class)).thenReturn("TEACHER");
        when(claims.get("username", String.class)).thenReturn("teacher01");

        HeaderEnhancerFilter filter = new HeaderEnhancerFilter(jwtUtil, new JwtTokenResolver(), "test-gateway-secret");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/v1/api/examruntime-service/ws?access_token=ws-token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = currentExchange -> {
            forwardedExchange.set(currentExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("teacher-id");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("TEACHER");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Gateway-Secret"))
                .isEqualTo("test-gateway-secret");
    }

    @Test
    void ignoresAccessTokenOnNonWebSocketPaths() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        HeaderEnhancerFilter filter = new HeaderEnhancerFilter(jwtUtil, new JwtTokenResolver(), "test-gateway-secret");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/v1/api/examruntime-service/student/exams?access_token=ws-token")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = currentExchange -> {
            forwardedExchange.set(currentExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Gateway-Secret")).isNull();
    }
}
