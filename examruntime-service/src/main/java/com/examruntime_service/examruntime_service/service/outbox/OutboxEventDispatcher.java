package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.model.dto.events.SubmissionCreatedEvent;
import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxEventDispatcher {

    private final SubmissionCreatedKafkaPublisher submissionPublisher;
    private final OutboxStateService stateService;
    private final ObjectMapper objectMapper;

    public OutboxEventDispatcher(
            SubmissionCreatedKafkaPublisher submissionPublisher,
            OutboxStateService stateService,
            ObjectMapper objectMapper
    ) {
        this.submissionPublisher = submissionPublisher;
        this.stateService = stateService;
        this.objectMapper = objectMapper;
    }

    public void dispatch(OutboxEvent event) throws Exception {
        if (!SubmissionCreatedEvent.EVENT_TYPE.equals(event.getEventType())) {
            throw new IllegalArgumentException("UNSUPPORTED_OUTBOX_EVENT: " + event.getEventType());
        }
        // Payload da duoc dong bang trong transaction submit; dispatcher chi deserialize va gui ra Kafka.
        SubmissionCreatedEvent payload = objectMapper.readValue(event.getPayload(), SubmissionCreatedEvent.class);
        submissionPublisher.publish(event.getId(), payload);
        // Chi danh dau PUBLISHED sau khi Kafka ack thanh cong.
        stateService.published(event.getId());
    }
}
