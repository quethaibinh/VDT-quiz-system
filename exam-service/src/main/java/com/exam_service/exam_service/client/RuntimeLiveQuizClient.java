package com.exam_service.exam_service.client;

import com.exam_service.exam_service.model.entity.enums.LiveQuizJoinPolicy;
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
 * Goi Exam Runtime Service de tao room live quiz sau khi Exam Service da commit snapshot.
 *
 * Client nay chi phu trach boundary HTTP. Nghiep vu prepare nam o service de viec
 * retry va idempotency ro rang hon.
 */
public class RuntimeLiveQuizClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public RuntimeLiveQuizClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.examruntime.base-url:http://localhost:8084}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    public LiveQuizRoomResponse createRoom(
            UUID examId,
            UUID ownerTeacherId,
            int snapshotVersion,
            LiveQuizJoinPolicy joinPolicy,
            String quizTitle,
            String subjectName,
            int questionCount,
            boolean showLeaderboard
    ) {
        try {
            String body = client.post()
                    .uri("/v1/internal/examruntime-service/live-quizzes/rooms")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(new LiveQuizRoomRequest(
                            examId,
                            ownerTeacherId,
                            snapshotVersion,
                            joinPolicy.name(),
                            quizTitle,
                            subjectName,
                            questionCount,
                            showLeaderboard
                    ))
                    .retrieve()
                    .body(String.class);
            // Gateway/global handler co the boc response trong field data; test/local co the tra DTO thang.
            JsonNode data = objectMapper.readTree(body).path("data");
            if (data.isMissingNode() || data.isNull()) {
                data = objectMapper.readTree(body);
            }
            return objectMapper.treeToValue(data, LiveQuizRoomResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                // 4xx nghia la Runtime tu choi contract/payload, khong phai loi ha tang tam thoi.
                throw new IllegalArgumentException("LIVE_QUIZ_ROOM_CREATE_REJECTED");
            }
            // 5xx duoc day len dang unavailable de caller co the retry prepare sau.
            throw new IllegalStateException("EXAM_RUNTIME_SERVICE_UNAVAILABLE");
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("EXAM_RUNTIME_SERVICE_UNAVAILABLE");
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_EXAM_RUNTIME_RESPONSE", exception);
        }
    }

    private record LiveQuizRoomRequest(
            UUID examId,
            UUID ownerTeacherId,
            int snapshotVersion,
            String joinPolicy,
            String quizTitle,
            String subjectName,
            int questionCount,
            boolean showLeaderboard
    ) {
    }

    public record LiveQuizRoomResponse(
            UUID roomId,
            UUID examId,
            String roomCode,
            String quizTitle,
            String subjectName,
            int questionCount,
            boolean showLeaderboard,
            String status
    ) {
    }
}
