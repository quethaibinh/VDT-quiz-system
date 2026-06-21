package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.dto.outbox.ExamSnapshotCacheRequested;
import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.model.entity.enums.OutboxStatus;
import com.exam_service.exam_service.repository.OutboxEventRepo;
import com.exam_service.exam_service.service.exams.ExamSchedulingTransactionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "exam.outbox.enabled",
        havingValue = "true",
        matchIfMissing = true
)
/**
 * Poll outbox theo batch nho de Redis loi khong chan luong chot lich.
 */
public class OutboxPollingWorker {

    private static final List<OutboxStatus> CLAIMABLE = List.of(
            OutboxStatus.PENDING,
            OutboxStatus.FAILED,
            OutboxStatus.PROCESSING
    );
    private final OutboxEventRepo outboxRepo;
    private final OutboxStateService stateService;
    private final ExamSnapshotCachePublisher publisher;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public OutboxPollingWorker(
            OutboxEventRepo outboxRepo,
            OutboxStateService stateService,
            ExamSnapshotCachePublisher publisher,
            ObjectMapper objectMapper,
            @Value("${exam.outbox.batch-size:20}") int batchSize
    ) {
        this.outboxRepo = outboxRepo;
        this.stateService = stateService;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${exam.outbox.poll-delay-ms:5000}")
    // lap lich, cu moi 5 giay la check bang outbox 1 lan de xu ly neu co ban ghi outbox moi va dang khong bi khoa
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> events = outboxRepo.findClaimable(
                CLAIMABLE, now, PageRequest.of(0, batchSize)
        );
        for (OutboxEvent event : events) {
            process(event, now);
        }
    }

    private void process(OutboxEvent event, LocalDateTime now) {
        // neu dang bi khoa trong worker khac roi thi bo qua
        if (!stateService.claim(event.getId(), now, now.plusMinutes(2))) {
            return;
        }
        try {
            if (!ExamSchedulingTransactionService.CACHE_EVENT.equals(event.getEventType())) {
                throw new IllegalArgumentException("UNSUPPORTED_OUTBOX_EVENT");
            }
            ExamSnapshotCacheRequested request = objectMapper.readValue( // mapping
                    event.getPayload(), ExamSnapshotCacheRequested.class
            );
            // neu ca thi da het han thi khong can xu ly ban ghi nay trong bang outbox
            if (!request.expiresAt().isAfter(OffsetDateTime.now())) {
                stateService.expired(event.getId());
                return;
            }
            publisher.publish(request); // neu co ban ghi trong outbox chu xu ly va chua het han thi xu ly
            stateService.published(event.getId());
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
