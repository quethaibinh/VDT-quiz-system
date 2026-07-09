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
/**
 * Xac thuc JWT va chuyen claims thanh Authentication cua Spring Security.
 */
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    /**
     * Tra ve Authentication rong khi token het han, sai chu ky hoac claims khong hop le.
     */
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        try {
            if (jwtUtil.isTokenExpired(token)) {
                return Mono.empty();
            }

            // Chi tin cac truong du lieu sau khi JwtUtil da xac minh chu ky token.
            Claims claims = jwtUtil.getClaims(token);
            String username = claims.getSubject();
            String role = normalizeRole(claims.get("userRole", String.class));
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

            return Mono.just(new UsernamePasswordAuthenticationToken(username, token, authorities));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    // hasRole() cua Spring Security yeu cau quyen co tien to ROLE_.
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }

        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
