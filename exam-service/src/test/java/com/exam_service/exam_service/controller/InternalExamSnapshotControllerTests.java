package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.config.security.ExamHeaderAuthenticationFilter;
import com.exam_service.exam_service.config.security.InternalApiKeyAuthenticationFilter;
import com.exam_service.exam_service.config.security.SecurityConfig;
import com.exam_service.exam_service.model.dto.cache.ExamPaperPoolDTO;
import com.exam_service.exam_service.model.dto.cache.RuntimeActivationDTO;
import com.exam_service.exam_service.service.exams.ExamSnapshotReadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalExamSnapshotController.class)
@Import({
        SecurityConfig.class,
        ExamHeaderAuthenticationFilter.class,
        InternalApiKeyAuthenticationFilter.class
})
@TestPropertySource(properties = "services.internal-api-key=test-internal-api-key")
class InternalExamSnapshotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamSnapshotReadService snapshotReadService;

    @Test
    void protectsInternalSnapshotAndReturnsPaperWithoutAnswers() throws Exception {
        UUID examId = UUID.randomUUID();
        mockMvc.perform(get("/v1/internal/exam-service/exams/{examId}/paper-pool", examId))
                .andExpect(status().isUnauthorized());

        when(snapshotReadService.getPaperPool(examId))
                .thenReturn(new ExamPaperPoolDTO(examId, 1, 1, 0, 0, List.of()));

        mockMvc.perform(get("/v1/internal/exam-service/exams/{examId}/paper-pool", examId)
                        .header(
                                InternalApiKeyAuthenticationFilter.INTERNAL_API_KEY_HEADER,
                                "test-internal-api-key"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examId").value(examId.toString()))
                .andExpect(jsonPath("$.data.snapshotVersion").value(1))
                .andExpect(jsonPath("$.data.answers").doesNotExist());
    }

    @Test
    void returnsRuntimeActivationMetadataForRepair() throws Exception {
        UUID examId = UUID.randomUUID();
        when(snapshotReadService.getRuntimeActivation(examId))
                .thenReturn(new RuntimeActivationDTO(
                        examId,
                        1,
                        OffsetDateTime.parse("2026-07-01T08:00:00Z"),
                        OffsetDateTime.parse("2026-07-01T09:00:00Z"),
                        10,
                        15
                ));

        mockMvc.perform(get("/v1/internal/exam-service/exams/{examId}/runtime-activation", examId)
                        .header(
                                InternalApiKeyAuthenticationFilter.INTERNAL_API_KEY_HEADER,
                                "test-internal-api-key"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examId").value(examId.toString()))
                .andExpect(jsonPath("$.data.snapshotVersion").value(1))
                .andExpect(jsonPath("$.data.joinBeforeMinutes").value(10))
                .andExpect(jsonPath("$.data.joinAfterMinutes").value(15));
    }
}
