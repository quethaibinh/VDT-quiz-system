package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.model.dto.events.SubmissionCreatedEvent;
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
public class SubmissionCreatedKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionCreatedKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final long timeoutMs;

    public SubmissionCreatedKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${examruntime.kafka.topic.submission:runtime-submission-events}") String topic,
            @Value("${examruntime.kafka.publish-timeout-ms:5000}") long timeoutMs
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.timeoutMs = timeoutMs;
    }

    public void publish(UUID eventId, SubmissionCreatedEvent payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to serialize SubmissionCreatedEvent", exception);
        }

        try {
            // Dung examId lam Kafka key de cac submission cung bai thi giu thu tu trong cung partition.
            kafkaTemplate.send(topic, payload.examId().toString(), payloadJson)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("Published SubmissionCreatedEvent for eventId: {}", eventId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing SubmissionCreatedEvent", exception);
        } catch (ExecutionException exception) {
            throw new RuntimeException("Failed to publish SubmissionCreatedEvent", exception.getCause());
        } catch (TimeoutException exception) {
            throw new RuntimeException("Timed out while publishing SubmissionCreatedEvent", exception);
        }
    }
}
