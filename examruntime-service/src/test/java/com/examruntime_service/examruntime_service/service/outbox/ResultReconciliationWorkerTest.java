package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.client.ResultServiceResultClient;
import com.examruntime_service.examruntime_service.model.dto.events.SubmissionCreatedEvent;
import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResultReconciliationWorkerTest {

    private OutboxEventRepo outboxRepo;
    private ResultServiceResultClient resultClient;
    private OutboxEventDispatcher dispatcher;
    private OutboxStateService stateService;
    private ResultReconciliationWorker worker;

    @BeforeEach
    void setUp() {
        outboxRepo = mock(OutboxEventRepo.class);
        resultClient = mock(ResultServiceResultClient.class);
        dispatcher = mock(OutboxEventDispatcher.class);
        stateService = mock(OutboxStateService.class);
        worker = new ResultReconciliationWorker(outboxRepo, resultClient, dispatcher, stateService, 10, 0);
    }

    @Test
    void republishOnlySubmissionsMissingFromResultService() throws Exception {
        OutboxEvent graded = event(UUID.randomUUID());
        OutboxEvent missing = event(UUID.randomUUID());
        when(outboxRepo.findPublishedForReconciliation(
                eq(SubmissionCreatedEvent.EVENT_TYPE),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(graded, missing));
        when(resultClient.findGradedSubmissionIds(List.of(graded.getAggregateId(), missing.getAggregateId())))
                .thenReturn(Set.of(graded.getAggregateId()));

        worker.republishMissingResults();

        verify(dispatcher, never()).dispatch(graded);
        verify(dispatcher).dispatch(missing);
    }

    @Test
    void skipsWhenResultStatusLookupFails() {
        OutboxEvent event = event(UUID.randomUUID());
        when(outboxRepo.findPublishedForReconciliation(
                eq(SubmissionCreatedEvent.EVENT_TYPE),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(event));
        when(resultClient.findGradedSubmissionIds(List.of(event.getAggregateId())))
                .thenThrow(new IllegalStateException("RESULT_STATUS_LOOKUP_FAILED"));

        worker.republishMissingResults();

        verifyNoInteractions(dispatcher);
        verifyNoInteractions(stateService);
    }

    @Test
    void marksRepublishFailureForNormalOutboxRetry() throws Exception {
        OutboxEvent missing = event(UUID.randomUUID());
        when(outboxRepo.findPublishedForReconciliation(
                eq(SubmissionCreatedEvent.EVENT_TYPE),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(missing));
        when(resultClient.findGradedSubmissionIds(List.of(missing.getAggregateId()))).thenReturn(Set.of());
        doThrow(new RuntimeException("Kafka unavailable")).when(dispatcher).dispatch(missing);

        worker.republishMissingResults();

        verify(stateService).failed(eq(missing.getId()), eq("Kafka unavailable"), any(LocalDateTime.class));
    }

    private OutboxEvent event(UUID submissionId) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(submissionId);
        event.setEventType(SubmissionCreatedEvent.EVENT_TYPE);
        event.setPublishedAt(LocalDateTime.now().minusMinutes(1));
        return event;
    }
}
