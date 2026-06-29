package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.client.ResultServiceResultClient;
import com.examruntime_service.examruntime_service.model.dto.events.SubmissionCreatedEvent;
import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "examruntime.result-reconciliation.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ResultReconciliationWorker {

    private static final Logger logger = LoggerFactory.getLogger(ResultReconciliationWorker.class);

    private final OutboxEventRepo outboxRepo;
    private final ResultServiceResultClient resultClient;
    private final OutboxEventDispatcher dispatcher;
    private final OutboxStateService stateService;
    private final int batchSize;
    private final long publishedAgeMs;

    public ResultReconciliationWorker(
            OutboxEventRepo outboxRepo,
            ResultServiceResultClient resultClient,
            OutboxEventDispatcher dispatcher,
            OutboxStateService stateService,
            @Value("${examruntime.result-reconciliation.batch-size:50}") int batchSize,
            @Value("${examruntime.result-reconciliation.published-age-ms:15000}") long publishedAgeMs
    ) {
        this.outboxRepo = outboxRepo;
        this.resultClient = resultClient;
        this.dispatcher = dispatcher;
        this.stateService = stateService;
        this.batchSize = batchSize;
        this.publishedAgeMs = publishedAgeMs;
    }

    @Scheduled(fixedDelayString = "${examruntime.result-reconciliation.poll-delay-ms:30000}")
    public void republishMissingResults() {
        LocalDateTime publishedBefore = LocalDateTime.now().minus(Duration.ofMillis(publishedAgeMs));
        List<OutboxEvent> events = outboxRepo.findPublishedForReconciliation(
                SubmissionCreatedEvent.EVENT_TYPE,
                publishedBefore,
                PageRequest.of(0, batchSize)
        );
        if (events.isEmpty()) {
            return;
        }

        List<UUID> submissionIds = events.stream()
                .map(OutboxEvent::getAggregateId)
                .toList();
        Set<UUID> gradedSubmissionIds;
        try {
            gradedSubmissionIds = resultClient.findGradedSubmissionIds(submissionIds);
        } catch (RuntimeException exception) {
            logger.warn("Skipped result reconciliation because Result Service status lookup failed: {}",
                    exception.getMessage());
            return;
        }
        for (OutboxEvent event : events) {
            if (gradedSubmissionIds.contains(event.getAggregateId())) {
                continue;
            }
            republish(event);
        }
    }

    private void republish(OutboxEvent event) {
        try {
            dispatcher.dispatch(event);
            logger.warn("Republished SubmissionCreated eventId={} submissionId={} after result reconciliation miss",
                    event.getId(), event.getAggregateId());
        } catch (Exception exception) {
            long delaySeconds = Math.min(300, 5L << Math.min(event.getRetryCount(), 6));
            stateService.failed(
                    event.getId(),
                    exception.getMessage(),
                    LocalDateTime.now().plus(Duration.ofSeconds(delaySeconds))
            );
        }
    }
}
