package com.result_service.result_service.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
/**
 * Bao ve API noi bo khong di qua Gateway.
 */
public class InternalApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/v1/internal/";
    private final byte[] configuredApiKey;

    public InternalApiKeyAuthenticationFilter(
            @Value("${services.internal-api-key:}") String configuredApiKey
    ) {
        this.configuredApiKey = configuredApiKey.getBytes(StandardCharsets.UTF_8);
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
        String suppliedApiKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        boolean valid = configuredApiKey.length > 0
                && suppliedApiKey != null
                && MessageDigest.isEqual(
                configuredApiKey,
                suppliedApiKey.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
