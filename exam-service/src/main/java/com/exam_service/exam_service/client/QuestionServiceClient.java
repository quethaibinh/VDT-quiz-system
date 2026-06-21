package com.exam_service.exam_service.client;

import com.exam_service.exam_service.util.exception.NotFoundException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
/**
 * Goi Question Service de kiem tra bo cau hoi va quota co the dung cho ca thi.
 */
public class QuestionServiceClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public QuestionServiceClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.question.base-url}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    /**
     * Lay metadata toi thieu; khong tai noi dung cau hoi hoac dap an ve Exam Service.
     */
    public QuestionCollectionMetadata getExamMetadata(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId
    ) {
        try {
            String body = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/internal/question-service/subjects/{subjectId}/collections/{collectionId}/exam-metadata")
                            .queryParam("teacherId", teacherId)
                            .build(subjectId, collectionId))
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(body).path("data");
            return objectMapper.treeToValue(data, QuestionCollectionMetadata.class);
        } catch (RestClientResponseException exception) {
            // Giu NOT_FOUND cho collection bi an; cac loi 4xx con lai la lua chon khong hop le.
            if (exception.getStatusCode().value() == 404) {
                throw new NotFoundException("COLLECTION_NOT_FOUND");
            }
            if (exception.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("INVALID_COLLECTION_SELECTION");
            }
            throw new IllegalStateException("QUESTION_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            // Timeout/ket noi loi duoc chuyen thanh ma loi nghiep vu on dinh.
            throw new IllegalStateException("QUESTION_SERVICE_UNAVAILABLE");
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_QUESTION_SERVICE_RESPONSE");
        }
    }

    /**
     * Lay candidate pool day du de Exam Service dong bang khi chot lich.
     */
    public QuestionCollectionSnapshot getExamSnapshot(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId
    ) {
        try {
            String body = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/internal/question-service/subjects/{subjectId}/collections/{collectionId}/exam-snapshot")
                            .queryParam("teacherId", teacherId)
                            .build(subjectId, collectionId))
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(body).path("data");
            return objectMapper.treeToValue(data, QuestionCollectionSnapshot.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new NotFoundException("COLLECTION_NOT_FOUND");
            }
            if (exception.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("INVALID_COLLECTION_SNAPSHOT");
            }
            throw new IllegalStateException("QUESTION_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("QUESTION_SERVICE_UNAVAILABLE");
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_QUESTION_SERVICE_RESPONSE");
        }
    }
}
