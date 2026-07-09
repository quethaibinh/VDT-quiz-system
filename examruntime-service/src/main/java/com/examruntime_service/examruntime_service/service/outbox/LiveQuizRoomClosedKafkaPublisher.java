package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.model.dto.events.LiveQuizRoomClosedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class LiveQuizRoomClosedKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LiveQuizRoomClosedKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final long timeoutMs;

    public LiveQuizRoomClosedKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${examruntime.kafka.topic.live-quiz-result:live-quiz-result-events}") String topic,
            @Value("${examruntime.kafka.publish-timeout-ms:5000}") long timeoutMs
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.timeoutMs = timeoutMs;
    }

    public void publish(UUID eventId, LiveQuizRoomClosedEvent payload) {
        // Publisher chi chiu trach nhiem day event da dong bang ra Kafka.
        // Logic tinh diem/rank nam o LiveQuizCloseFinalizationService de dispatcher khong co business rule.
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to serialize LiveQuizRoomClosedEvent", exception);
        }

        try {
            // Dung roomId lam key de cac event cua cung phong giu thu tu trong Kafka.
            kafkaTemplate.send(topic, payload.roomId().toString(), payloadJson)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("Published LiveQuizRoomClosedEvent for eventId: {}", eventId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing LiveQuizRoomClosedEvent", exception);
        } catch (ExecutionException exception) {
            throw new RuntimeException("Failed to publish LiveQuizRoomClosedEvent", exception.getCause());
        } catch (TimeoutException exception) {
            throw new RuntimeException("Timed out while publishing LiveQuizRoomClosedEvent", exception);
        }
    }
}
