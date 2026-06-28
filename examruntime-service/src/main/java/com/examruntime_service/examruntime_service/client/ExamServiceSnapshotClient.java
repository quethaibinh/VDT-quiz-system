package com.examruntime_service.examruntime_service.client;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.RuntimeActivationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
/**
 * Client noi bo dung khi Runtime khong thay paper-pool trong Redis.
 * Exam Service van la source of truth, nen fallback doc snapshot tu API noi bo.
 */
public class ExamServiceSnapshotClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public ExamServiceSnapshotClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.exam.base-url}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    public ExamPaperPoolDTO getPaperPool(UUID examId) {
        try {
            String body = client.get()
                    .uri("/v1/internal/exam-service/exams/{examId}/paper-pool", examId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(String.class);

            // Controller thanh cong trong service duoc boc bang ApiResponse,
            // nhung test/client tuong lai co the tra DTO thuan. Ho tro ca hai dang.
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return objectMapper.treeToValue(data, ExamPaperPoolDTO.class);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new IllegalStateException("EXAM_SERVICE_SNAPSHOT_UNAVAILABLE", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_EXAM_SERVICE_SNAPSHOT_RESPONSE", exception);
        }
    }

    public ExamAnswerKeyDTO getAnswerKey(UUID examId) {
        try {
            String body = client.get()
                    .uri("/v1/internal/exam-service/exams/{examId}/answer-key", examId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return objectMapper.treeToValue(data, ExamAnswerKeyDTO.class);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new IllegalStateException("EXAM_SERVICE_ANSWER_KEY_UNAVAILABLE", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_EXAM_SERVICE_ANSWER_KEY_RESPONSE", exception);
        }
    }

    public RuntimeActivationDTO getRuntimeActivation(UUID examId) {
        try {
            String body = client.get()
                    .uri("/v1/internal/exam-service/exams/{examId}/runtime-activation", examId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return objectMapper.treeToValue(data, RuntimeActivationDTO.class);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new IllegalStateException("EXAM_SERVICE_RUNTIME_ACTIVATION_UNAVAILABLE", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_EXAM_SERVICE_RUNTIME_ACTIVATION_RESPONSE", exception);
        }
    }
}
