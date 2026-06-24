package com.question_service.question_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
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

    public ResolveTeachersResponse resolveTeachers(List<UUID> teacherIds) {
        try {
            String body = client.post()
                    .uri("/v1/internal/auth-service/teachers/resolve")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("teacherIds", teacherIds))
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(body).path("data");
            ResolveTeachersResponse response = new ResolveTeachersResponse(
                    List.of(objectMapper.treeToValue(data.path("teachers"), TeacherSummary[].class)),
                    List.of(objectMapper.treeToValue(data.path("missingTeacherIds"), UUID[].class)),
                    List.of(objectMapper.treeToValue(data.path("nonTeacherIds"), UUID[].class))
            );
            validateCompleteResponse(teacherIds, response);
            return response;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_SERVICE_UNAVAILABLE");
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_SERVICE_UNAVAILABLE");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "INVALID_AUTH_SERVICE_RESPONSE");
        }
    }

    private void validateCompleteResponse(List<UUID> requestedIds, ResolveTeachersResponse response) {
        Set<UUID> returnedIds = new HashSet<>();
        response.teachers().forEach(teacher -> {
            if (teacher == null || teacher.id() == null || !returnedIds.add(teacher.id())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "INVALID_AUTH_SERVICE_RESPONSE");
            }
        });
        response.missingTeacherIds().forEach(id -> addUnique(returnedIds, id));
        response.nonTeacherIds().forEach(id -> addUnique(returnedIds, id));
        if (requestedIds.size() != returnedIds.size()
                || !returnedIds.equals(new HashSet<>(requestedIds))) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "INVALID_AUTH_SERVICE_RESPONSE");
        }
    }

    private void addUnique(Set<UUID> ids, UUID id) {
        if (id == null || !ids.add(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "INVALID_AUTH_SERVICE_RESPONSE");
        }
    }
}
