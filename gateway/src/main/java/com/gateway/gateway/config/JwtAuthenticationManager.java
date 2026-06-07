package com.gateway.gateway.config;

import com.gateway.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;

    // Spring inject JwtUtil qua constructor.
    public JwtAuthenticationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        try {
            if (jwtUtil.isTokenExpired(token)) {
                return Mono.empty();
            }

            // Lấy thông tin user từ token do auth-service tạo.
            Claims claims = jwtUtil.getClaims(token);
            String username = claims.getSubject();
            String role = normalizeRole(claims.get("userRole", String.class));
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

            // Tạo Authentication hợp lệ để Spring Security dùng cho phân quyền.
            return Mono.just(new UsernamePasswordAuthenticationToken(username, token, authorities));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    // Spring Security cần role có tiền tố ROLE_ khi dùng hasRole().
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }

        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
