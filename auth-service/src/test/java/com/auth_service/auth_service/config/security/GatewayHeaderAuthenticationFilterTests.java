package com.auth_service.auth_service.config.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GatewayHeaderAuthenticationFilterTests {

    private final GatewayHeaderAuthenticationFilter filter =
            new GatewayHeaderAuthenticationFilter("test-gateway-secret");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAdminAuthenticationFromCompleteHeaders() throws Exception {
        MockHttpServletRequest request = trustedRequest("user-1", "admin01", "ADMIN");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_ADMIN");
        assertThat(authentication.getCredentials()).isNull();

        GatewayUserPrincipal principal = (GatewayUserPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo("user-1");
        assertThat(principal.username()).isEqualTo("admin01");
        assertThat(principal.role()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void normalizesSupportedRoles() throws Exception {
        MockHttpServletRequest request = trustedRequest("user-2", "teacher01", "ROLE_TEACHER");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    void createsStudentAuthority() throws Exception {
        MockHttpServletRequest request = trustedRequest("user-3", "student01", "STUDENT");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void doesNotAuthenticateWhenARequiredHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "user-1");
        request.addHeader(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "ADMIN");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticateSpoofedIdentityHeadersWithoutGatewaySecret() throws Exception {
        MockHttpServletRequest request = trustedRequest("user-1", "admin01", "ADMIN");
        request.removeHeader(GatewayHeaderAuthenticationFilter.GATEWAY_SECRET_HEADER);

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAuthenticateBlankOrUnsupportedRole() throws Exception {
        MockHttpServletRequest blankRole = trustedRequest("user-1", "admin01", " ");

        filter.doFilter(blankRole, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest unsupportedRole = trustedRequest("user-1", "admin01", "SUPER_ADMIN");
        filter.doFilter(unsupportedRole, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotOverwriteExistingAuthentication() throws Exception {
        Authentication existing = UsernamePasswordAuthenticationToken.authenticated(
                "existing-user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(existing);

        filter.doFilter(
                trustedRequest("user-1", "admin01", "ADMIN"),
                new MockHttpServletResponse(),
                mock(FilterChain.class)
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    }

    private MockHttpServletRequest trustedRequest(String userId, String username, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, userId);
        request.addHeader(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, username);
        request.addHeader(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, role);
        request.addHeader(GatewayHeaderAuthenticationFilter.GATEWAY_SECRET_HEADER, "test-gateway-secret");
        return request;
    }
}
