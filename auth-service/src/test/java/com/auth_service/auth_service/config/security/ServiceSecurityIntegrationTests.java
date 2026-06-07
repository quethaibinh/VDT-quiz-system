package com.auth_service.auth_service.config.security;

import com.auth_service.auth_service.controller.AdminUserImportController;
import com.auth_service.auth_service.controller.AuthController;
import com.auth_service.auth_service.model.dto.imports.ImportUserResultDTO;
import com.auth_service.auth_service.service.auths.UserService;
import com.auth_service.auth_service.service.imports.UserImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, AdminUserImportController.class})
@Import({SecurityConfig.class, GatewayHeaderAuthenticationFilter.class})
class ServiceSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserImportService userImportService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void allowsPublicLoginWithoutTrustedHeaders() throws Exception {
        when(userService.login(any())).thenReturn("token");

        mockMvc.perform(post("/v1/api/auth-service/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "student01",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicRegisterWithoutTrustedHeaders() throws Exception {
        mockMvc.perform(post("/v1/api/auth-service/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "student01",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void returnsUnauthorizedWhenAdminEndpointHasNoTrustedHeaders() throws Exception {
        mockMvc.perform(importRequest())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsForbiddenWhenAuthenticatedUserIsNotAdmin() throws Exception {
        mockMvc.perform(importRequest()
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "user-1")
                        .header(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, "student01")
                        .header(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "STUDENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminToImportUsers() throws Exception {
        when(userImportService.importUsers(any())).thenReturn(new ImportUserResultDTO());

        mockMvc.perform(importRequest()
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "user-2")
                        .header(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, "admin01")
                        .header(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "ADMIN"))
                .andExpect(status().isOk());

        verify(userImportService).importUsers(any());
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder importRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1}
        );
        return multipart("/v1/api/auth-service/admin/users/import").file(file);
    }
}
