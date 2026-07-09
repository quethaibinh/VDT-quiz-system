package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.dto.outbox.ExamActivatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExamActivatedKafkaPublisherTests {

    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private ExamActivatedKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        publisher = new ExamActivatedKafkaPublisher(kafkaTemplate, objectMapper, "test-topic", 500);
    }

    @Test
    void publishesEventSuccessfully() throws Exception {
        UUID examId = UUID.randomUUID();
        ExamActivatedEvent payload = new ExamActivatedEvent(
                UUID.randomUUID(), ExamActivatedEvent.EVENT_TYPE, ExamActivatedEvent.EVENT_VERSION,
                examId, 0, OffsetDateTime.now(), null, 10, 0, OffsetDateTime.now()
        );

        when(objectMapper.writeValueAsString(payload)).thenReturn("{}");

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("test-topic"), eq(examId.toString()), eq("{}"))).thenReturn(future);

        publisher.publish(UUID.randomUUID(), payload);

        verify(kafkaTemplate).send("test-topic", examId.toString(), "{}");
    }

    @Test
    void throwsWhenPublishFails() throws Exception {
        UUID examId = UUID.randomUUID();
        ExamActivatedEvent payload = new ExamActivatedEvent(
                UUID.randomUUID(), ExamActivatedEvent.EVENT_TYPE, ExamActivatedEvent.EVENT_VERSION,
                examId, 0, OffsetDateTime.now(), null, 10, 0, OffsetDateTime.now()
        );

        when(objectMapper.writeValueAsString(payload)).thenReturn("{}");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Broker down"));
        when(kafkaTemplate.send(eq("test-topic"), eq(examId.toString()), eq("{}"))).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(UUID.randomUUID(), payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish ExamActivatedEvent")
                .hasRootCauseMessage("Broker down");
    }

    @Test
    void throwsOnTimeout() throws Exception {
        UUID examId = UUID.randomUUID();
        ExamActivatedEvent payload = new ExamActivatedEvent(
                UUID.randomUUID(), ExamActivatedEvent.EVENT_TYPE, ExamActivatedEvent.EVENT_VERSION,
                examId, 0, OffsetDateTime.now(), null, 10, 0, OffsetDateTime.now()
        );

        when(objectMapper.writeValueAsString(payload)).thenReturn("{}");

        // A future that never completes
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("test-topic"), eq(examId.toString()), eq("{}"))).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(UUID.randomUUID(), payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Timed out while publishing ExamActivatedEvent");
    }
}
