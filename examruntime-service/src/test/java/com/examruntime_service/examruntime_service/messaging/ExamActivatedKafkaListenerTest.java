package com.examruntime_service.examruntime_service.messaging;

import com.examruntime_service.examruntime_service.model.dto.events.ExamActivatedEvent;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationWarmupService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamActivatedKafkaListenerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void acknowledgesOnlyAfterWarmupSucceeds() throws Exception {
        RuntimeActivationWarmupService warmupService = mock(RuntimeActivationWarmupService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ExamActivatedEvent event = event();
        String rawJson = objectMapper.writeValueAsString(event);
        when(warmupService.handle(rawJson))
                .thenReturn(new RuntimeActivationWarmupService.WarmupResult(
                        true,
                        event.examId(),
                        RuntimeActivationMetadata.SOURCE_REDIS
                ));

        ExamActivatedKafkaListener listener = new ExamActivatedKafkaListener(warmupService, objectMapper);
        listener.onMessage(new ConsumerRecord<>("exam-lifecycle-events", 0, 1, event.examId().toString(), rawJson),
                acknowledgment);

        verify(warmupService).handle(rawJson);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotAcknowledgeWhenWarmupFails() throws Exception {
        RuntimeActivationWarmupService warmupService = mock(RuntimeActivationWarmupService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ExamActivatedEvent event = event();
        String rawJson = objectMapper.writeValueAsString(event);
        when(warmupService.handle(rawJson)).thenThrow(new IllegalStateException("REDIS_UNAVAILABLE"));

        ExamActivatedKafkaListener listener = new ExamActivatedKafkaListener(warmupService, objectMapper);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        listener.onMessage(
                                new ConsumerRecord<>("exam-lifecycle-events", 0, 1, event.examId().toString(), rawJson),
                                acknowledgment
                        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("REDIS_UNAVAILABLE");

        verify(acknowledgment, never()).acknowledge();
    }

    private ExamActivatedEvent event() {
        return new ExamActivatedEvent(
                UUID.randomUUID(),
                ExamActivatedEvent.EVENT_TYPE,
                ExamActivatedEvent.EVENT_VERSION,
                UUID.randomUUID(),
                1,
                OffsetDateTime.parse("2026-07-01T08:00:00+07:00"),
                OffsetDateTime.parse("2026-07-01T09:00:00+07:00"),
                15,
                10,
                OffsetDateTime.parse("2026-06-24T11:00:00+07:00")
        );
    }
}
