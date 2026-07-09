package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "examruntime.outbox.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPollingWorker {

    private static final List<OutboxStatus> CLAIMABLE = List.of(
            OutboxStatus.PENDING,
            OutboxStatus.FAILED,
            OutboxStatus.PROCESSING
    );

    private final OutboxEventRepo outboxRepo;
    private final OutboxStateService stateService;
    private final OutboxEventDispatcher dispatcher;
    private final int batchSize;

    public OutboxPollingWorker(
            OutboxEventRepo outboxRepo,
            OutboxStateService stateService,
            OutboxEventDispatcher dispatcher,
            @Value("${examruntime.outbox.batch-size:20}") int batchSize
    ) {
        this.outboxRepo = outboxRepo;
        this.stateService = stateService;
        this.dispatcher = dispatcher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${examruntime.outbox.poll-delay-ms:5000}")
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        // Lay cac event co the claim, bao gom event FAILED den han retry.
        List<OutboxEvent> events = outboxRepo.findClaimable(
                CLAIMABLE,
                now,
                PageRequest.of(0, batchSize)
        );
        for (OutboxEvent event : events) {
            process(event, now);
        }
    }

    private void process(OutboxEvent event, LocalDateTime now) {
        // Claim truoc khi publish de nhieu instance worker khong gui trung cung event.
        if (!stateService.claim(event.getId(), now, now.plusMinutes(2))) {
            return;
        }
        try {
            dispatcher.dispatch(event);
        } catch (Exception exception) {
            // Backoff luy thua co tran 5 phut de Kafka/Result Service hoi phuc ma khong spam retry.
            long delaySeconds = Math.min(300, 5L << Math.min(event.getRetryCount(), 6));
            stateService.failed(
                    event.getId(),
                    exception.getMessage(),
                    LocalDateTime.now().plus(Duration.ofSeconds(delaySeconds))
            );
        }
    }
}
