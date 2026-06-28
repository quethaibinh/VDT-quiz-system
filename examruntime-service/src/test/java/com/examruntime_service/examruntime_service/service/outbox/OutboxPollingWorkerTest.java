package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutboxPollingWorkerTest {

    private OutboxEventRepo outboxRepo;
    private OutboxStateService stateService;
    private OutboxEventDispatcher dispatcher;
    private OutboxPollingWorker worker;

    @BeforeEach
    void setUp() {
        outboxRepo = mock(OutboxEventRepo.class);
        stateService = mock(OutboxStateService.class);
        dispatcher = mock(OutboxEventDispatcher.class);
        worker = new OutboxPollingWorker(outboxRepo, stateService, dispatcher, 10);
    }

    @Test
    void skipsEventIfNotClaimed() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        when(outboxRepo.findClaimable(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(stateService.claim(eq(event.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        worker.publishPending();

        verifyNoInteractions(dispatcher);
    }

    @Test
    void dispatchesClaimedEvent() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        when(outboxRepo.findClaimable(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(stateService.claim(eq(event.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        worker.publishPending();

        verify(dispatcher).dispatch(event);
    }

    @Test
    void marksFailureForRetryWhenDispatchThrows() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setRetryCount(1);
        when(outboxRepo.findClaimable(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(stateService.claim(eq(event.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);
        doThrow(new RuntimeException("Dispatch error")).when(dispatcher).dispatch(event);

        worker.publishPending();

        verify(stateService).failed(eq(event.getId()), eq("Dispatch error"), any(LocalDateTime.class));
    }
}
