package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorRealtimeMessageDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
/**
 * Nhan Redis Pub/Sub message va day ra WebSocket client dang ket noi instance hien tai.
 *
 * Vi teacher co the connect vao instance B trong khi student event duoc xu ly o instance A,
 * subscriber nay la cau noi giua Redis backplane va STOMP simple broker local.
 */
public class MonitorRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public MonitorRedisSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(String payload) {
        try {
            MonitorRealtimeMessageDTO message = objectMapper.readValue(payload, MonitorRealtimeMessageDTO.class);
            if (message.examId() == null) {
                return;
            }
            // Tat ca monitor message deu di vao topic chung cua ca thi cho teacher dashboard.
            messagingTemplate.convertAndSend(MonitorDestinations.teacherTopic(message.examId()), message);
            if (message.alert() != null && message.studentId() != null) {
                // Alert rieng nhu LOCKED chi gui vao user queue cua student do.
                messagingTemplate.convertAndSendToUser(
                        message.studentId().toString(),
                        MonitorDestinations.studentAlertQueue(message.examId()),
                        message.alert()
                );
            }
        } catch (Exception ignored) {
            // Pub/Sub is best effort. Malformed monitor messages are skipped.
        }
    }
}
