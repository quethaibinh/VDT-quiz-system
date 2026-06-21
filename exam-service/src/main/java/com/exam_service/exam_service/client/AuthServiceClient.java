package com.exam_service.exam_service.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
/**
 * Goi Auth Service qua kenh noi bo de xac minh hoc sinh va lay snapshot danh tinh.
 */
public class AuthServiceClient {

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

    /**
     * Resolve toan bo student ID trong mot request de tranh tin ten/ma do browser gui.
     */
    public List<ResolvedStudent> resolveStudents(List<UUID> studentIds) {
        try {
            String body = client.post()
                    .uri("/v1/internal/auth-service/students/resolve")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("studentIds", studentIds))
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(body).path("data");
            // Khong luu mot phan danh sach neu Auth bao co ID loi.
            if (!data.path("missingStudentIds").isEmpty()
                    || !data.path("inactiveStudentIds").isEmpty()
                    || !data.path("nonStudentIds").isEmpty()) {
                throw new IllegalArgumentException("INVALID_STUDENT_SELECTION");
            }
            ResolvedStudent[] students = objectMapper.treeToValue(
                    data.path("students"),
                    ResolvedStudent[].class
            );
            return List.of(students);
        } catch (RestClientResponseException exception) {
            // Loi 4xx tu Auth co nghia lua chon hoc sinh khong con hop le.
            throw new IllegalArgumentException("INVALID_STUDENT_SELECTION");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            // Tach loi ha tang de controller tra thong bao on dinh thay vi lo chi tiet HTTP.
            throw new IllegalStateException("AUTH_SERVICE_UNAVAILABLE");
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_AUTH_SERVICE_RESPONSE");
        }
    }
}
