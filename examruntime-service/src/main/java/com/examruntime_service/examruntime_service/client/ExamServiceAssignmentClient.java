package com.examruntime_service.examruntime_service.client;

import com.examruntime_service.examruntime_service.model.dto.cache.InternalExamAssignmentDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
// Client goi sang exam-service de lay phan cong cua hoc sinh khi bi cache miss o Redis
public class ExamServiceAssignmentClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public ExamServiceAssignmentClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.exam.base-url}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    public InternalExamAssignmentDTO getAssignments(UUID examId) {
        try {
            String body = client.get()
                    .uri("/v1/internal/exam-service/exams/{examId}/assignments", examId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return objectMapper.treeToValue(data, InternalExamAssignmentDTO.class);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new IllegalStateException("EXAM_SERVICE_ASSIGNMENT_UNAVAILABLE", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_EXAM_SERVICE_ASSIGNMENT_RESPONSE", exception);
        }
    }
}
