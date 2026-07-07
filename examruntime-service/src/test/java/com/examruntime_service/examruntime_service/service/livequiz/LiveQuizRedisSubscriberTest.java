package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizLeaderboardEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LiveQuizRedisSubscriberTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LiveQuizRedisSubscriber subscriber = new LiveQuizRedisSubscriber(messagingTemplate, objectMapper);

    @Test
    void routesRoomClosedToLobbyTeacherProgressAndLeaderboardWhenLeaderboardIsPresent() throws Exception {
        UUID roomId = UUID.randomUUID();
        LiveQuizRealtimeMessageDTO message = new LiveQuizRealtimeMessageDTO(
                roomId,
                UUID.randomUUID(),
                "ROOM_CLOSED",
                OffsetDateTime.parse("2026-07-07T08:00:00Z"),
                LiveQuizRoomStatus.CLOSED,
                null,
                null,
                List.of(leaderboardEntry()),
                null
        );

        subscriber.handleMessage(objectMapper.writeValueAsString(message));

        verify(messagingTemplate).convertAndSend(LiveQuizDestinations.lobbyTopic(roomId), message);
        verify(messagingTemplate).convertAndSend(LiveQuizDestinations.teacherProgressTopic(roomId), message);
        verify(messagingTemplate).convertAndSend(LiveQuizDestinations.leaderboardTopic(roomId), message);
        verify(messagingTemplate, never()).convertAndSendToUser(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any());
    }

    @Test
    void ignoresMalformedPayload() {
        subscriber.handleMessage("{not-json");

        verifyNoInteractions(messagingTemplate);
    }

    private LiveQuizLeaderboardEntryDTO leaderboardEntry() {
        return new LiveQuizLeaderboardEntryDTO(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Student Demo",
                BigDecimal.TEN,
                10,
                8,
                0,
                1200,
                true
        );
    }
}
