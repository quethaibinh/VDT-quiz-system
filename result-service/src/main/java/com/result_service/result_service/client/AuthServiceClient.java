package com.result_service.result_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AuthServiceClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public AuthServiceClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.auth.base-url}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    public Map<UUID, StudentSummary> resolveStudents(List<UUID> studentIds) {
        List<UUID> ids = studentIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            String body = client.post()
                    .uri("/v1/internal/auth-service/students/resolve")
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("studentIds", ids))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            StudentSummary[] students = objectMapper.treeToValue(data.path("students"), StudentSummary[].class);
            return List.of(students).stream()
                    .filter(student -> student != null && student.id() != null)
                    .collect(Collectors.toMap(StudentSummary::id, student -> student, (left, right) -> left));
        } catch (RestClientResponseException | ResourceAccessException exception) {
            return Map.of();
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
