package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.config.security.GatewayHeaderAuthenticationFilter;
import com.auth_service.auth_service.config.security.InternalApiKeyAuthenticationFilter;
import com.auth_service.auth_service.config.security.SecurityConfig;
import com.auth_service.auth_service.model.dto.students.ResolveStudentsResponseDTO;
import com.auth_service.auth_service.model.dto.students.StudentPageResponseDTO;
import com.auth_service.auth_service.model.dto.students.StudentSummaryDTO;
import com.auth_service.auth_service.service.students.StudentDiscoveryService;
import com.auth_service.auth_service.service.teachers.TeacherResolutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TeacherStudentController.class,
        InternalStudentController.class,
        InternalTeacherController.class
})
@Import({
        SecurityConfig.class,
        GatewayHeaderAuthenticationFilter.class,
        InternalApiKeyAuthenticationFilter.class
})
@TestPropertySource(properties = "app.security.internal-api-key=test-internal-key")
class StudentControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentDiscoveryService studentDiscoveryService;

    @MockitoBean
    private TeacherResolutionService teacherResolutionService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void teacherCanSearchAndResponseContainsOnlySafeStudentFields() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentDiscoveryService.search(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(new StudentPageResponseDTO(
                        List.of(new StudentSummaryDTO(id, "SV001", "Student One", "Student")),
                        0, 20, 1, 1, true, true
                ));

        mockMvc.perform(get("/v1/api/auth-service/teacher/students")
                        .param("keyword", "student")
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "teacher-id")
                        .header(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, "teacher01")
                        .header(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "TEACHER")
                        .header(GatewayHeaderAuthenticationFilter.GATEWAY_SECRET_HEADER, "test-gateway-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data.content[0].studentCode").value("SV001"))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Student One"))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Student"))
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].nationalId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist());
    }

    @Test
    void searchRejectsMissingOrNonTeacherIdentity() throws Exception {
        mockMvc.perform(get("/v1/api/auth-service/teacher/students"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/api/auth-service/teacher/students")
                        .header(GatewayHeaderAuthenticationFilter.USER_ID_HEADER, "student-id")
                        .header(GatewayHeaderAuthenticationFilter.USERNAME_HEADER, "student01")
                        .header(GatewayHeaderAuthenticationFilter.USER_ROLE_HEADER, "STUDENT")
                        .header(GatewayHeaderAuthenticationFilter.GATEWAY_SECRET_HEADER, "test-gateway-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalResolveRequiresCorrectApiKey() throws Exception {
        String body = """
                {"studentIds":["00000000-0000-0000-0000-000000000001"]}
                """;

        mockMvc.perform(post("/v1/internal/auth-service/students/resolve")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/v1/internal/auth-service/students/resolve")
                        .header(InternalApiKeyAuthenticationFilter.INTERNAL_API_KEY_HEADER, "wrong-key")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalResolveAcceptsCorrectApiKey() throws Exception {
        when(studentDiscoveryService.resolve(any())).thenReturn(
                new ResolveStudentsResponseDTO(List.of(), List.of(), List.of(), List.of())
        );

        mockMvc.perform(post("/v1/internal/auth-service/students/resolve")
                        .header(InternalApiKeyAuthenticationFilter.INTERNAL_API_KEY_HEADER, "test-internal-key")
                        .contentType("application/json")
                        .content("""
                                {"studentIds":["00000000-0000-0000-0000-000000000001"]}
                                """))
                .andExpect(status().isOk());

        verify(studentDiscoveryService).resolve(any());
    }

    @Test
    void internalTeacherResolveRequiresInternalApiKey() throws Exception {
        mockMvc.perform(post("/v1/internal/auth-service/teachers/resolve")
                        .contentType("application/json")
                        .content("""
                                {"teacherIds":["00000000-0000-0000-0000-000000000001"]}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
