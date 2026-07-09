package com.result_service.result_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

@Component
public class ExamServiceClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public ExamServiceClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.exam.base-url}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    public Optional<ExamPaperPoolDTO> getPaperPool(UUID examId) {
        try {
            String body = client.get()
                    .uri("/v1/internal/exam-service/exams/{examId}/paper-pool", examId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return Optional.ofNullable(objectMapper.treeToValue(data, ExamPaperPoolDTO.class));
        } catch (RestClientResponseException | ResourceAccessException exception) {
            return Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public Optional<RuntimeActivationDTO> getRuntimeActivation(UUID examId) {
        try {
            String body = client.get()
                    .uri("/v1/internal/exam-service/exams/{examId}/runtime-activation", examId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            return Optional.ofNullable(objectMapper.treeToValue(data, RuntimeActivationDTO.class));
        } catch (RestClientResponseException | ResourceAccessException exception) {
            return Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
