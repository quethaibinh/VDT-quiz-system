package com.examruntime_service.examruntime_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuestionMediaClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public QuestionMediaClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.question.base-url}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    public Map<String, String> signedUrls(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return Map.of();
        }
        try {
            String body = client.post()
                    .uri("/v1/internal/question-service/question-media/urls")
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .body(Map.of("objectKeys", objectKeys))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.path("data") : root;
            JsonNode urlsNode = data.path("urls");
            if (!urlsNode.isObject()) {
                return Map.of();
            }
            Map<String, String> urls = new LinkedHashMap<>();
            urlsNode.properties().forEach(entry -> urls.put(entry.getKey(), entry.getValue().asText()));
            return urls;
        } catch (RestClientResponseException | ResourceAccessException exception) {
            return Map.of();
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
