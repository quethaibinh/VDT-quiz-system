package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.model.dto.events.SubmissionCreatedEvent;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionCreatedKafkaPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private SubmissionCreatedKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        publisher = new SubmissionCreatedKafkaPublisher(kafkaTemplate, objectMapper, "test-topic", 500);
    }

    @Test
    void publishesWithExamIdKey() throws Exception {
        SubmissionCreatedEvent event = event();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("test-topic"), eq(event.examId().toString()), eq("{}"))).thenReturn(future);

        publisher.publish(event.eventId(), event);

        verify(kafkaTemplate).send("test-topic", event.examId().toString(), "{}");
    }

    @Test
    void throwsWhenKafkaPublishFails() throws Exception {
        SubmissionCreatedEvent event = event();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Broker down"));
        when(kafkaTemplate.send(eq("test-topic"), eq(event.examId().toString()), eq("{}"))).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(event.eventId(), event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish SubmissionCreatedEvent")
                .hasRootCauseMessage("Broker down");
    }

    private SubmissionCreatedEvent event() {
        return new SubmissionCreatedEvent(
                UUID.randomUUID(),
                SubmissionCreatedEvent.EVENT_TYPE,
                SubmissionCreatedEvent.PRODUCER,
                OffsetDateTime.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                1,
                SubmitReason.STUDENT,
                OffsetDateTime.now(),
                List.of(),
                Map.of()
        );
    }
}
