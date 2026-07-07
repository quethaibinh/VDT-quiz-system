package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRealtimeMessageDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class LiveQuizRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Inject STOMP messaging template de day message tu Redis ra websocket.
     */
    public LiveQuizRedisSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Nhan message tu Redis va fan-out sang cac STOMP destination phu hop.
     */
    public void handleMessage(String payload) {
        try {
            LiveQuizRealtimeMessageDTO message = objectMapper.readValue(payload, LiveQuizRealtimeMessageDTO.class);
            if (message.roomId() == null) {
                return;
            }
            String type = message.type() != null ? message.type() : "";
            if (type.equals("PARTICIPANT_JOINED") || type.equals("ROOM_STARTED") || type.equals("ROOM_CLOSED")) {
                // Lobby chi can su kien join va bat dau phong.
                messagingTemplate.convertAndSend(LiveQuizDestinations.lobbyTopic(message.roomId()), message);
            }
            if (type.equals("QUESTION_STARTED")
                    || type.equals("ANSWER_SUBMITTED")
                    || type.equals("QUESTION_TIMEOUT")
                    || type.equals("PARTICIPANT_FINISHED")
                    || type.equals("ROOM_STARTED")
                    || type.equals("ROOM_CLOSED")) {
                // Teacher progress nhan cac su kien lam bai theo thoi gian thuc.
                messagingTemplate.convertAndSend(LiveQuizDestinations.teacherProgressTopic(message.roomId()), message);
            }
            if (message.leaderboard() != null) {
                // Leaderboard chi fan-out khi service da tinh bang xep hang moi.
                messagingTemplate.convertAndSend(LiveQuizDestinations.leaderboardTopic(message.roomId()), message);
            }
            if (message.studentTargetId() != null) {
                // Queue rieng de gui update ve dung student, khong lo dap an/timeout bi broadcast.
                messagingTemplate.convertAndSendToUser(
                        message.studentTargetId().toString(),
                        LiveQuizDestinations.studentQueue(message.roomId()),
                        message
                );
            }
        } catch (Exception ignored) {
            // Pub/Sub la best effort; message live quiz sai format thi bo qua.
        }
    }
}
