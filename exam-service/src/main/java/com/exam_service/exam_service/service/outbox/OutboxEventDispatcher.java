package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.dto.outbox.ExamActivatedEvent;
import com.exam_service.exam_service.model.dto.outbox.ExamSnapshotCacheRequested;
import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.service.exams.ExamSchedulingTransactionService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

@Component
public class OutboxEventDispatcher {

    private final ExamSnapshotCachePublisher snapshotPublisher;
    private final ExamActivatedKafkaPublisher activatedPublisher;
    private final OutboxStateService stateService;
    private final ObjectMapper objectMapper;

    public OutboxEventDispatcher(
            ExamSnapshotCachePublisher snapshotPublisher,
            ExamActivatedKafkaPublisher activatedPublisher,
            OutboxStateService stateService,
            ObjectMapper objectMapper) {
        this.snapshotPublisher = snapshotPublisher;
        this.activatedPublisher = activatedPublisher;
        this.stateService = stateService;
        this.objectMapper = objectMapper;
    }

    // ham dung de xu ly event outbox, neu la cache -> redis, neu la event -> kafka
    public void dispatch(OutboxEvent event) throws Exception {
        if (ExamSchedulingTransactionService.CACHE_EVENT.equals(event.getEventType())) {
            ExamSnapshotCacheRequested request = objectMapper.readValue(
                    event.getPayload(), ExamSnapshotCacheRequested.class
            );
            if (!request.expiresAt().isAfter(OffsetDateTime.now())) {
                stateService.expired(event.getId());
                return;
            }
            snapshotPublisher.publish(request);
            stateService.published(event.getId());

        } else if (ExamActivatedEvent.EVENT_TYPE.equals(event.getEventType())) {
            ExamActivatedEvent payload = objectMapper.readValue(
                    event.getPayload(), ExamActivatedEvent.class
            );
            activatedPublisher.publish(event.getId(), payload);
            stateService.published(event.getId());

        } else {
            throw new IllegalArgumentException("UNSUPPORTED_OUTBOX_EVENT: " + event.getEventType());
        }
    }
}
