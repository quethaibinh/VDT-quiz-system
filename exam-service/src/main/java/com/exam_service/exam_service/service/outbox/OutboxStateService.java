package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.model.entity.enums.OutboxStatus;
import com.exam_service.exam_service.repository.OutboxEventRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
/**
 * Tach cac transaction trang thai outbox khoi ket noi Redis.
 */
public class OutboxStateService {

    private static final List<OutboxStatus> CLAIMABLE = List.of(
            OutboxStatus.PENDING,
            OutboxStatus.FAILED,
            OutboxStatus.PROCESSING
    );
    private final OutboxEventRepo outboxRepo;

    public OutboxStateService(OutboxEventRepo outboxRepo) {
        this.outboxRepo = outboxRepo;
    }

    @Transactional
    // check lieu co worker nao dang xu ly roi khong
    public boolean claim(UUID eventId, LocalDateTime now, LocalDateTime leaseUntil) {
        return outboxRepo.claim(eventId, CLAIMABLE, now, leaseUntil) == 1;
    }

    @Transactional
    // chuyen trang thai outbox sang published
    public void published(UUID eventId) {
        OutboxEvent event = outboxRepo.findById(eventId).orElseThrow();
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setNextRetryAt(null);
        event.setLastError(null);
    }

    @Transactional
    // outbox fail va setup retry
    public void failed(UUID eventId, String error, LocalDateTime nextRetryAt) {
        OutboxEvent event = outboxRepo.findById(eventId).orElseThrow();
        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setNextRetryAt(nextRetryAt);
        event.setLastError(error == null ? "UNKNOWN_DELIVERY_ERROR" : error.substring(
                0, Math.min(error.length(), 1000)
        ));
    }

    @Transactional
    // chuyen trang thai outbox het han
    public void expired(UUID eventId) {
        OutboxEvent event = outboxRepo.findById(eventId).orElseThrow();
        event.setStatus(OutboxStatus.EXPIRED);
        event.setNextRetryAt(null);
        event.setLastError("OUTBOX_EVENT_EXPIRED");
    }
}
