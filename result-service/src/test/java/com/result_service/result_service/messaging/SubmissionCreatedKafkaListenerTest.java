package com.result_service.result_service.messaging;

import com.result_service.result_service.service.grading.SubmissionGradingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionCreatedKafkaListenerTest {

    @Test
    void acknowledgesOnlyAfterGradingSucceeds() {
        SubmissionGradingService gradingService = mock(SubmissionGradingService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        String rawJson = "{\"eventType\":\"SubmissionCreated\"}";
        when(gradingService.handle(rawJson)).thenReturn(
                new SubmissionGradingService.GradingOutcome(true, UUID.randomUUID(), UUID.randomUUID())
        );

        SubmissionCreatedKafkaListener listener = new SubmissionCreatedKafkaListener(gradingService);
        listener.onMessage(new ConsumerRecord<>("runtime-submission-events", 0, 1, "key", rawJson), acknowledgment);

        verify(gradingService).handle(rawJson);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotAcknowledgeWhenGradingFails() {
        SubmissionGradingService gradingService = mock(SubmissionGradingService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        String rawJson = "{\"eventType\":\"SubmissionCreated\"}";
        when(gradingService.handle(rawJson)).thenThrow(new IllegalArgumentException("PAPER_SNAPSHOT_QUESTIONS_REQUIRED"));

        SubmissionCreatedKafkaListener listener = new SubmissionCreatedKafkaListener(gradingService);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        listener.onMessage(
                                new ConsumerRecord<>("runtime-submission-events", 0, 1, "key", rawJson),
                                acknowledgment
                        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PAPER_SNAPSHOT_QUESTIONS_REQUIRED");

        verify(acknowledgment, never()).acknowledge();
    }
}
