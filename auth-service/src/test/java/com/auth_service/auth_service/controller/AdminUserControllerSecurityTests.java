package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.config.security.GatewayHeaderAuthenticationFilter;
import com.auth_service.auth_service.config.security.InternalApiKeyAuthenticationFilter;
import com.auth_service.auth_service.config.security.SecurityConfig;
import com.auth_service.auth_service.model.dto.users.AdminUserPageResponseDTO;
import com.auth_service.auth_service.service.users.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@Import({
        SecurityConfig.class,
        GatewayHeaderAuthenticationFilter.class,
        InternalApiKeyAuthenticationFilter.class
})
class AdminUserControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void adminUserRoutesRequireAdminTrustedHeaders() throws Exception {
        mockMvc.perform(get("/v1/api/admin/auth-service/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/api/admin/auth-service/users")
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "teacher-id")
                        .header(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, "teacher01")
                        .header(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "TEACHER")
                        .header(GatewayHeaderAuthenticationFilter.GATEWAY_SECRET_HEADER, "test-gateway-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearchUsers() throws Exception {
        when(adminUserService.search(any(), any(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new AdminUserPageResponseDTO(
                        List.of(), 0, 20, 0, 0, true, true
                ));

        mockMvc.perform(get("/v1/api/admin/auth-service/users")
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "admin-id")
                        .header(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, "admin01")
                        .header(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "ADMIN")
                        .header(GatewayHeaderAuthenticationFilter.GATEWAY_SECRET_HEADER, "test-gateway-secret"))
                .andExpect(status().isOk());
    }

    @Test
    void malformedAdminMutationReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/v1/api/admin/auth-service/users/00000000-0000-0000-0000-000000000001/status")
                        .contentType("application/json")
                        .content("{\"status\":")
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "admin-id")
                        .header(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, "admin01")
                        .header(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "ADMIN")
                        .header(GatewayHeaderAuthenticationFilter.GATEWAY_SECRET_HEADER, "test-gateway-secret"))
                .andExpect(status().isBadRequest());
    }
}
