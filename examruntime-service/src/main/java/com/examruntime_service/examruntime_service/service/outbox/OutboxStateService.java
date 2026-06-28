package com.examruntime_service.examruntime_service.service.outbox;

import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
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
    public boolean claim(UUID eventId, LocalDateTime now, LocalDateTime leaseUntil) {
        return outboxRepo.claim(eventId, CLAIMABLE, now, leaseUntil) == 1;
    }

    @Transactional
    public void published(UUID eventId) {
        OutboxEvent event = outboxRepo.findById(eventId).orElseThrow();
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setNextRetryAt(null);
        event.setLastError(null);
    }

    @Transactional
    public void failed(UUID eventId, String error, LocalDateTime nextRetryAt) {
        OutboxEvent event = outboxRepo.findById(eventId).orElseThrow();
        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setNextRetryAt(nextRetryAt);
        event.setLastError(error == null ? "UNKNOWN_DELIVERY_ERROR" : error.substring(0, Math.min(error.length(), 1000)));
    }
}
