package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.dto.outbox.ExamActivatedEvent;
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
public class ExamActivatedKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ExamActivatedKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final long timeoutMs;

    public ExamActivatedKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${exam.kafka.topic.exam-lifecycle:exam-lifecycle-events}") String topic,
            @Value("${exam.kafka.publish-timeout-ms:5000}") long timeoutMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.timeoutMs = timeoutMs;
    }

    public void publish(UUID eventId, ExamActivatedEvent payload) throws Exception {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ExamActivatedEvent", e);
        }

        try {
            kafkaTemplate.send(topic, payload.examId().toString(), payloadJson)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("Published ExamActivatedEvent for eventId: {}", eventId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing ExamActivatedEvent", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to publish ExamActivatedEvent", e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out while publishing ExamActivatedEvent", e);
        }
    }
}
