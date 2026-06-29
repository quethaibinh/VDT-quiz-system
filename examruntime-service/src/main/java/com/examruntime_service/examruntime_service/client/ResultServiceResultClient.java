package com.examruntime_service.examruntime_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ResultServiceResultClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public ResultServiceResultClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.result.base-url:http://result-service:8085}") String resultServiceBaseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(resultServiceBaseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    public Set<UUID> findGradedSubmissionIds(Collection<UUID> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return Set.of();
        }
        try {
            String body = client.post()
                    .uri("/v1/internal/result-service/results/status")
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .body(new ResultStatusRequest(List.copyOf(submissionIds)))
                    .retrieve()
                    .body(String.class);
            return parseGradedSubmissionIds(body);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new IllegalStateException("RESULT_STATUS_LOOKUP_FAILED", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_RESULT_STATUS_RESPONSE", exception);
        }
    }

    Set<UUID> parseGradedSubmissionIds(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return Set.of();
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.has("data") ? root.path("data") : root;
        ResultStatusResponse response = objectMapper.treeToValue(data, ResultStatusResponse.class);
        if (response == null || response.gradedSubmissionIds() == null) {
            return Set.of();
        }
        return new HashSet<>(response.gradedSubmissionIds());
    }

    public record ResultStatusRequest(List<UUID> submissionIds) {
    }

    public record ResultStatusResponse(List<UUID> gradedSubmissionIds) {
    }
}
