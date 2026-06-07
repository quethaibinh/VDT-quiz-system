package com.gateway.gateway.config;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtAuthenticationManager authenticationManager;

    // Repository này lấy SecurityContext từ JWT trong mỗi request.
    public SecurityContextRepository(JwtAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        // Gateway chạy stateless nên không lưu SecurityContext vào session.
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        // Đọc token từ header Authorization: Bearer <token>.
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(token, token);

            // Xác thực token rồi đưa kết quả vào SecurityContext.
            return authenticationManager.authenticate(authRequest)
                    .map(SecurityContextImpl::new);
        }

        return Mono.empty();
    }
}
