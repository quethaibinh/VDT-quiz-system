package com.exam_service.exam_service.client;

import com.exam_service.exam_service.model.entity.enums.LiveQuizJoinPolicy;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
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
            UUID subjectId,
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
                            subjectId,
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

    public List<LiveQuizRoomLookupResponse> latestRooms(List<UUID> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        try {
            String body = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/internal/examruntime-service/live-quizzes/rooms/latest")
                            .queryParam("examIds", examIds.toArray())
                            .build())
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(body).path("data");
            if (data.isMissingNode() || data.isNull()) {
                data = objectMapper.readTree(body);
            }
            LiveQuizRoomLookupResponse[] rooms = objectMapper.treeToValue(data, LiveQuizRoomLookupResponse[].class);
            return rooms == null ? List.of() : List.of(rooms);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            // Lookup nay chi phuc vu UI action. Neu Runtime tam loi, van tra danh sach quiz de giao vien khong bi chan.
            return List.of();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private record LiveQuizRoomRequest(
            UUID examId,
            UUID ownerTeacherId,
            int snapshotVersion,
            String joinPolicy,
            String quizTitle,
            UUID subjectId,
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
            UUID subjectId,
            String subjectName,
            int questionCount,
            boolean showLeaderboard,
            String status
    ) {
    }

    public record LiveQuizRoomLookupResponse(
            UUID examId,
            UUID roomId,
            String roomCode,
            String status
    ) {
    }
}
