package com.exam_service.exam_service.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
/**
 * Tao Authentication tu identity header do gateway da xac thuc.
 */
public class ExamHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USERNAME_HEADER = "X-Username";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private static final String ROLE_PREFIX = "ROLE_";
    private static final Set<String> SUPPORTED_ROLES = Set.of("ADMIN", "TEACHER", "STUDENT");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            createAuthentication(request);
        }

        filterChain.doFilter(request, response);
    }

    private void createAuthentication(HttpServletRequest request) {
        String userId = trimToNull(request.getHeader(USER_ID_HEADER));
        String username = trimToNull(request.getHeader(USERNAME_HEADER));
        String role = normalizeRole(request.getHeader(USER_ROLE_HEADER));

        // Bo qua request neu header dinh danh thieu hoac vai tro khong duoc ho tro.
        if (userId == null || username == null || role == null) {
            return;
        }

        ExamUserPrincipal principal = new ExamUserPrincipal(userId, username, role);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String normalizeRole(String value) {
        String role = trimToNull(value);
        if (role == null) {
            return null;
        }

        role = role.toUpperCase(Locale.ROOT);
        if (role.startsWith(ROLE_PREFIX)) {
            role = role.substring(ROLE_PREFIX.length());
        }

        return SUPPORTED_ROLES.contains(role) ? ROLE_PREFIX + role : null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
