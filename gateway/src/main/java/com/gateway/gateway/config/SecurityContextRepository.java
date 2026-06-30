package com.gateway.gateway.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
/**
 * Tao SecurityContext tu JWT tren tung request, khong luu vao session.
 */
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtAuthenticationManager authenticationManager;
    private final JwtTokenResolver tokenResolver;

    public SecurityContextRepository(JwtAuthenticationManager authenticationManager, JwtTokenResolver tokenResolver) {
        this.authenticationManager = authenticationManager;
        this.tokenResolver = tokenResolver;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        // Gateway stateless nen khong luu SecurityContext sau request.
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String token = tokenResolver.resolve(exchange);
        if (token != null) {
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(token, token);

            return authenticationManager.authenticate(authRequest)
                    .map(SecurityContextImpl::new);
        }

        return Mono.empty();
    }
}
