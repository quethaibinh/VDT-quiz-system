package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.InternalApiKeyAuthenticationFilter;
import com.question_service.question_service.config.security.QuestionHeaderAuthenticationFilter;
import com.question_service.question_service.config.security.SecurityConfig;
import com.question_service.question_service.model.dto.collections.ExamCollectionMetadataDTO;
import com.question_service.question_service.service.collections.QuestionCollectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalExamCollectionController.class)
@Import({
        SecurityConfig.class,
        InternalApiKeyAuthenticationFilter.class,
        QuestionHeaderAuthenticationFilter.class
})
@TestPropertySource(properties = "internal.api-key=test-internal-api-key")
class InternalExamCollectionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionCollectionService collectionService;

    @Test
    void rejectsMissingOrWrongInternalApiKey() throws Exception {
        mockMvc.perform(request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                        .header(InternalApiKeyAuthenticationFilter.INTERNAL_API_KEY_HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsSafeMetadataForValidInternalRequest() throws Exception {
        UUID subjectId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(collectionService.getExamMetadata(subjectId, collectionId, teacherId))
                .thenReturn(new ExamCollectionMetadataDTO(
                        collectionId, subjectId, "Mathematics", "Exam pool", 3, 4, 5
                ));

        mockMvc.perform(request(subjectId, collectionId, teacherId)
                        .header(
                                InternalApiKeyAuthenticationFilter.INTERNAL_API_KEY_HEADER,
                                "test-internal-api-key"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collectionId").value(collectionId.toString()))
                .andExpect(jsonPath("$.data.subjectId").value(subjectId.toString()))
                .andExpect(jsonPath("$.data.subjectName").value("Mathematics"))
                .andExpect(jsonPath("$.data.name").value("Exam pool"))
                .andExpect(jsonPath("$.data.easy").value(3))
                .andExpect(jsonPath("$.data.medium").value(4))
                .andExpect(jsonPath("$.data.hard").value(5))
                .andExpect(jsonPath("$.data.content").doesNotExist())
                .andExpect(jsonPath("$.data.answer").doesNotExist());

        verify(collectionService).getExamMetadata(subjectId, collectionId, teacherId);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId
    ) {
        return get(
                "/v1/internal/question-service/subjects/{subjectId}/collections/{collectionId}/exam-metadata",
                subjectId,
                collectionId
        ).queryParam("teacherId", teacherId.toString());
    }
}
