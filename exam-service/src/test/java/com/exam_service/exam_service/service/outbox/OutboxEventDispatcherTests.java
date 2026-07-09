package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.dto.outbox.ExamActivatedEvent;
import com.exam_service.exam_service.model.dto.outbox.ExamSnapshotCacheRequested;
import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.service.exams.ExamSchedulingTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxEventDispatcherTests {

    private ExamSnapshotCachePublisher snapshotPublisher;
    private ExamActivatedKafkaPublisher activatedPublisher;
    private OutboxStateService stateService;
    private ObjectMapper objectMapper;
    private OutboxEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        snapshotPublisher = mock(ExamSnapshotCachePublisher.class);
        activatedPublisher = mock(ExamActivatedKafkaPublisher.class);
        stateService = mock(OutboxStateService.class);
        objectMapper = mock(ObjectMapper.class);
        dispatcher = new OutboxEventDispatcher(snapshotPublisher, activatedPublisher, stateService, objectMapper);
    }

    @Test
    void dispatchesSnapshotCacheRequestedEvent() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType(ExamSchedulingTransactionService.CACHE_EVENT);
        event.setPayload("{}");

        ExamSnapshotCacheRequested payload = new ExamSnapshotCacheRequested(
                UUID.randomUUID(), 0, OffsetDateTime.now().plusDays(1)
        );

        when(objectMapper.readValue("{}", ExamSnapshotCacheRequested.class)).thenReturn(payload);

        dispatcher.dispatch(event);

        verify(snapshotPublisher).publish(payload);
        verify(stateService).published(event.getId());
        verifyNoInteractions(activatedPublisher);
    }

    @Test
    void expiresSnapshotCacheIfPastExpiry() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType(ExamSchedulingTransactionService.CACHE_EVENT);
        event.setPayload("{}");

        ExamSnapshotCacheRequested payload = new ExamSnapshotCacheRequested(
                UUID.randomUUID(), 0, OffsetDateTime.now().minusDays(1)
        );

        when(objectMapper.readValue("{}", ExamSnapshotCacheRequested.class)).thenReturn(payload);

        dispatcher.dispatch(event);

        verify(stateService).expired(event.getId());
        verifyNoInteractions(snapshotPublisher);
    }

    @Test
    void dispatchesExamActivatedEvent() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType(ExamActivatedEvent.EVENT_TYPE);
        event.setPayload("{}");

        ExamActivatedEvent payload = new ExamActivatedEvent(
                UUID.randomUUID(), ExamActivatedEvent.EVENT_TYPE, ExamActivatedEvent.EVENT_VERSION,
                UUID.randomUUID(), 0, OffsetDateTime.now(), null, 10, 0, OffsetDateTime.now()
        );

        when(objectMapper.readValue("{}", ExamActivatedEvent.class)).thenReturn(payload);

        dispatcher.dispatch(event);

        verify(activatedPublisher).publish(event.getId(), payload);
        verify(stateService).published(event.getId());
        verifyNoInteractions(snapshotPublisher);
    }

    @Test
    void throwsOnUnknownEventType() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType("UnknownEvent");

        assertThatThrownBy(() -> dispatcher.dispatch(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNSUPPORTED_OUTBOX_EVENT: UnknownEvent");
    }
}
