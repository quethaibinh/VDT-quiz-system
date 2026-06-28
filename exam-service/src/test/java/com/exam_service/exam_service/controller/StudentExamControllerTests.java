package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.config.security.ExamHeaderAuthenticationFilter;
import com.exam_service.exam_service.config.security.InternalApiKeyAuthenticationFilter;
import com.exam_service.exam_service.config.security.SecurityConfig;
import com.exam_service.exam_service.model.dto.exams.StudentExamAvailability;
import com.exam_service.exam_service.model.dto.exams.StudentExamDetailDTO;
import com.exam_service.exam_service.model.dto.exams.StudentExamPageDTO;
import com.exam_service.exam_service.model.dto.exams.StudentExamSummaryDTO;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.service.exams.StudentExamReadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentExamController.class)
@Import({
        SecurityConfig.class,
        ExamHeaderAuthenticationFilter.class,
        InternalApiKeyAuthenticationFilter.class
})
class StudentExamControllerTests {

    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentExamReadService studentExamReadService;

    @Test
    void studentEndpointsRejectMissingOrInvalidRole() throws Exception {
        String path = "/v1/api/exam-service/student/exams";

        // Khong co headers
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());

        // Vai tro TEACHER goi vao endpoint hoc sinh
        mockMvc.perform(get(path)
                        .header(ExamHeaderAuthenticationFilter.USER_ID_HEADER, STUDENT_ID.toString())
                        .header(ExamHeaderAuthenticationFilter.USERNAME_HEADER, "teacher01")
                        .header(ExamHeaderAuthenticationFilter.USER_ROLE_HEADER, "TEACHER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAssignedExamsSuccessfully() throws Exception {
        String path = "/v1/api/exam-service/student/exams";
        
        StudentExamSummaryDTO summary = new StudentExamSummaryDTO(
                UUID.randomUUID(),
                "EX001",
                "Triet hoc",
                "Triet hoc dai cuong",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                60,
                10,
                ExamStatus.SCHEDULED,
                StudentExamAvailability.UPCOMING,
                AssignmentStatus.ASSIGNED
        );

        when(studentExamReadService.list(STUDENT_ID, "UPCOMING", 0, 20))
                .thenReturn(new StudentExamPageDTO(OffsetDateTime.now(), List.of(summary), 0, 20, 1, 1, true, true));

        mockMvc.perform(get(path)
                        .headers(studentHeaders())
                        .param("status", "UPCOMING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serverTime").exists())
                .andExpect(jsonPath("$.data.content[0].code").value("EX001"))
                .andExpect(jsonPath("$.data.content[0].studentAvailability").value("UPCOMING"));

        verify(studentExamReadService).list(STUDENT_ID, "UPCOMING", 0, 20);
    }

    @Test
    void getAssignedExamDetailSuccessfully() throws Exception {
        UUID examId = UUID.randomUUID();
        String path = "/v1/api/exam-service/student/exams/{examId}";

        StudentExamDetailDTO detail = new StudentExamDetailDTO(
                examId,
                "EX001",
                "Triet hoc",
                "Mo ta",
                "Triet hoc dai cuong",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                60,
                10,
                ExamStatus.SCHEDULED,
                StudentExamAvailability.UPCOMING,
                AssignmentStatus.ASSIGNED
        );

        when(studentExamReadService.get(examId, STUDENT_ID)).thenReturn(detail);

        mockMvc.perform(get(path, examId).headers(studentHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examId").value(examId.toString()))
                .andExpect(jsonPath("$.data.code").value("EX001"));

        verify(studentExamReadService).get(examId, STUDENT_ID);
    }

    private HttpHeaders studentHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ExamHeaderAuthenticationFilter.USER_ID_HEADER, STUDENT_ID.toString());
        headers.add(ExamHeaderAuthenticationFilter.USERNAME_HEADER, "student01");
        headers.add(ExamHeaderAuthenticationFilter.USER_ROLE_HEADER, "STUDENT");
        return headers;
    }
}
