package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.config.security.ExamHeaderAuthenticationFilter;
import com.exam_service.exam_service.config.security.InternalApiKeyAuthenticationFilter;
import com.exam_service.exam_service.config.security.SecurityConfig;
import com.exam_service.exam_service.service.assignments.ExamAssignmentService;
import com.exam_service.exam_service.service.exams.TeacherExamDraftService;
import com.exam_service.exam_service.service.exams.TeacherExamSchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

@WebMvcTest(controllers = {
        TeacherExamController.class,
        TeacherExamAssignmentController.class
})
@Import({
        SecurityConfig.class,
        ExamHeaderAuthenticationFilter.class,
        InternalApiKeyAuthenticationFilter.class
})
class TeacherExamControllerAcceptanceTests {

    private static final UUID TEACHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherExamDraftService examService;

    @MockitoBean
    private TeacherExamSchedulingService schedulingService;

    @MockitoBean
    private ExamAssignmentService assignmentService;

    @Test
    void teacherEndpointsRejectMissingAndNonTeacherIdentity() throws Exception {
        String path = "/v1/api/exam-service/teacher/subjects/{subjectId}/exams";
        UUID subjectId = UUID.randomUUID();

        mockMvc.perform(get(path, subjectId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(path, subjectId)
                        .header(ExamHeaderAuthenticationFilter.USER_ID_HEADER, UUID.randomUUID())
                        .header(ExamHeaderAuthenticationFilter.USERNAME_HEADER, "student01")
                        .header(ExamHeaderAuthenticationFilter.USER_ROLE_HEADER, "STUDENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherIdentityIsForwardedForDraftAndAssignmentMutations() throws Exception {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/v1/api/exam-service/teacher/subjects/{subjectId}/exams/{examId}/cancel",
                        subjectId,
                        examId
                ).headers(teacherHeaders()))
                .andExpect(status().isOk());
        mockMvc.perform(patch(
                        "/v1/api/exam-service/teacher/subjects/{subjectId}/exams/{examId}/schedule",
                        subjectId,
                        examId
                ).headers(teacherHeaders()))
                .andExpect(status().isOk());
        mockMvc.perform(delete(
                        "/v1/api/exam-service/teacher/subjects/{subjectId}/exams/{examId}/assignments/{studentId}",
                        subjectId,
                        examId,
                        studentId
                ).headers(teacherHeaders()))
                .andExpect(status().isOk());

        verify(examService).cancel(subjectId, examId, TEACHER_ID);
        verify(schedulingService).schedule(subjectId, examId, TEACHER_ID);
        verify(assignmentService).remove(subjectId, examId, TEACHER_ID, studentId);
    }

    @Test
    void createRejectsInvalidDraftRequestBeforeCallingService() throws Exception {
        UUID subjectId = UUID.randomUUID();

        mockMvc.perform(post(
                        "/v1/api/exam-service/teacher/subjects/{subjectId}/exams",
                        subjectId
                )
                        .headers(teacherHeaders())
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "",
                                  "collectionId": null,
                                  "easyCount": -1,
                                  "mediumCount": 0,
                                  "hardCount": 0,
                                  "durationMinutes": 0,
                                  "joinBeforeMinutes": -1,
                                  "joinAfterMinutes": 0,
                                  "showResultPolicy": "",
                                  "maxViolationAllowed": -1,
                                  "handleViolation": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(examService, never()).create(eq(subjectId), eq(TEACHER_ID), any());
    }

    @Test
    void updateRejectsInvalidValidatedFieldsBeforeCallingService() throws Exception {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        mockMvc.perform(put(
                        "/v1/api/exam-service/teacher/subjects/{subjectId}/exams/{examId}",
                        subjectId,
                        examId
                )
                        .headers(teacherHeaders())
                        .contentType("application/json")
                        .content(validDraftJson().replace(
                                "\"durationMinutes\": 60",
                                "\"durationMinutes\": 0"
                        )))
                .andExpect(status().isBadRequest());

        verify(examService, never()).update(eq(subjectId), eq(examId), eq(TEACHER_ID), any());
    }

    @Test
    void assignmentAddRejectsEmptyStudentSelectionBeforeCallingService() throws Exception {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        mockMvc.perform(post(
                        "/v1/api/exam-service/teacher/subjects/{subjectId}/exams/{examId}/assignments",
                        subjectId,
                        examId
                )
                        .headers(teacherHeaders())
                        .contentType("application/json")
                        .content("{\"studentIds\":[]}"))
                .andExpect(status().isBadRequest());

        verify(assignmentService, never()).add(
                eq(subjectId), eq(examId), eq(TEACHER_ID), any()
        );
    }

    private org.springframework.http.HttpHeaders teacherHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(ExamHeaderAuthenticationFilter.USER_ID_HEADER, TEACHER_ID.toString());
        headers.add(ExamHeaderAuthenticationFilter.USERNAME_HEADER, "teacher01");
        headers.add(ExamHeaderAuthenticationFilter.USER_ROLE_HEADER, "TEACHER");
        return headers;
    }

    private String validDraftJson() {
        return """
                {
                  "title": "Midterm",
                  "description": "Semester 1",
                  "collectionId": "00000000-0000-0000-0000-000000000201",
                  "easyCount": 1,
                  "mediumCount": 1,
                  "hardCount": 1,
                  "startAt": "2026-07-01T08:00:00+07:00",
                  "durationMinutes": 60,
                  "joinBeforeMinutes": 10,
                  "joinAfterMinutes": 0,
                  "shuffleQuestions": true,
                  "shuffleOptions": true,
                  "showResultPolicy": "AFTER_CLOSED",
                  "autoSubmit": true,
                  "requireFullscreen": true,
                  "maxViolationAllowed": 5,
                  "handleViolation": "LOCK"
                }
                """;
    }
}
