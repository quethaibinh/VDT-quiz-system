package com.auth_service.auth_service.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/v1/internal/";

    private final byte[] configuredApiKey;

    public InternalApiKeyAuthenticationFilter(
            @Value("${app.security.internal-api-key:}") String internalApiKey
    ) {
        this.configuredApiKey = internalApiKey == null || internalApiKey.isBlank()
                ? new byte[0]
                : internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String suppliedKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (configuredApiKey.length == 0
                || suppliedKey == null
                || !MessageDigest.isEqual(
                        configuredApiKey,
                        suppliedKey.getBytes(StandardCharsets.UTF_8)
                )) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "internal-service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
