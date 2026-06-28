package com.examruntime_service.examruntime_service.service.activation;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.events.ExamActivatedEvent;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
/**
 * Xu ly ExamActivated theo huong readiness warm-up:
 * validate event, dam bao paper-pool san sang, roi ghi metadata READY vao Redis.
 * Khong tao ExamSession o buoc nay; session chi tao khi hoc sinh join.
 */
public class RuntimeActivationWarmupService {

    // Ready data song den sau gio ket thuc de join/rejoin/submit muon van co cache.
    private static final Duration READY_RETENTION = Duration.ofHours(24);
    // Lock ngan de instance crash giua chung khong chan Kafka retry qua lau.
    private static final Duration PROCESSING_LOCK_TTL = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper;
    private final RuntimeActivationCache cache;
    private final ExamServiceSnapshotClient snapshotClient;
    private final Clock clock;

    public RuntimeActivationWarmupService(
            ObjectMapper objectMapper,
            RuntimeActivationCache cache,
            ExamServiceSnapshotClient snapshotClient,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.cache = cache;
        this.snapshotClient = snapshotClient;
        this.clock = clock;
    }

    public WarmupResult handle(String rawJson) {
        ExamActivatedEvent event = parse(rawJson);
        validate(event);
        Duration ttl = ttlUntil(event.endAt().plus(READY_RETENTION));

        // Duplicate eventId duoc skip: Kafka at-least-once co the deliver lai cung event.
        if (!cache.acquireProcessing(event.eventId(), PROCESSING_LOCK_TTL)) {
            return WarmupResult.duplicate(event.examId());
        }

        try {
            PaperPoolSnapshot snapshot = ensurePaperPool(event, ttl);
            // Metadata toi thieu cho join window sau nay; paper ca nhan van sinh on demand.
            RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                    event.examId(),
                    event.snapshotVersion(),
                    event.code(),
                    event.title(),
                    event.subjectId(),
                    event.subjectName(),
                    event.ownerTeacherId(),
                    event.startAt(),
                    event.endAt(),
                    event.joinBeforeMinutes(),
                    event.joinAfterMinutes(),
                    event.showResultPolicy(),
                    RuntimeActivationMetadata.STATUS_READY,
                    OffsetDateTime.now(clock),
                    snapshot.source()
            );
            cache.putActivation(metadata, ttl);
            cache.markProcessed(event.eventId(), ttl);
            return WarmupResult.processed(event.examId(), snapshot.source());
        } catch (RuntimeException exception) {
            // Xoa PROCESSING de record retry co the xu ly lai thay vi bi dedupe sai.
            cache.deleteInbox(event.eventId());
            throw exception;
        }
    }

    private PaperPoolSnapshot ensurePaperPool(ExamActivatedEvent event, Duration ttl) {
        String cached = cache.getPaperPool(event.examId(), event.snapshotVersion());
        if (cached != null && !cached.isBlank()) {
            // Redis hit van phai parse/verify de tranh mark READY voi snapshot sai version.
            ExamPaperPoolDTO paperPool = parsePaperPool(cached);
            verifyPaperPool(event, paperPool);
            return new PaperPoolSnapshot(paperPool, RuntimeActivationMetadata.SOURCE_REDIS, cached);
        }

        // Cache miss la recoverable: doc source of truth tu Exam Service va hydrate lai Redis.
        ExamPaperPoolDTO fallback = snapshotClient.getPaperPool(event.examId());
        verifyPaperPool(event, fallback);
        String payload = serializePaperPool(fallback);
        cache.putPaperPool(event.examId(), event.snapshotVersion(), payload, ttl);
        return new PaperPoolSnapshot(fallback, RuntimeActivationMetadata.SOURCE_EXAM_SERVICE_FALLBACK, payload);
    }

    private ExamActivatedEvent parse(String rawJson) {
        try {
            return objectMapper.readValue(rawJson, ExamActivatedEvent.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("INVALID_EXAM_ACTIVATED_EVENT_JSON", exception);
        }
    }

    private ExamPaperPoolDTO parsePaperPool(String rawJson) {
        try {
            return objectMapper.readValue(rawJson, ExamPaperPoolDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_PAPER_POOL_CACHE", exception);
        }
    }

    private String serializePaperPool(ExamPaperPoolDTO paperPool) {
        try {
            return objectMapper.writeValueAsString(paperPool);
        } catch (Exception exception) {
            throw new IllegalStateException("PAPER_POOL_SERIALIZATION_FAILED", exception);
        }
    }

    private void validate(ExamActivatedEvent event) {
        // Chi chap nhan contract ExamActivated v1 de tranh consumer hieu sai payload moi.
        if (event.eventId() == null || event.examId() == null) {
            throw new IllegalArgumentException("EXAM_ACTIVATED_EVENT_IDS_REQUIRED");
        }
        if (!ExamActivatedEvent.EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("UNSUPPORTED_EXAM_LIFECYCLE_EVENT_TYPE");
        }
        if (event.eventVersion() != ExamActivatedEvent.EVENT_VERSION) {
            throw new IllegalArgumentException("UNSUPPORTED_EXAM_ACTIVATED_EVENT_VERSION");
        }
        if (event.snapshotVersion() <= 0) {
            throw new IllegalArgumentException("INVALID_SNAPSHOT_VERSION");
        }
        if (event.startAt() == null || event.endAt() == null || !event.endAt().isAfter(event.startAt())) {
            throw new IllegalArgumentException("INVALID_EXAM_TIME_WINDOW");
        }
        if (event.joinBeforeMinutes() < 0 || event.joinAfterMinutes() < 0) {
            throw new IllegalArgumentException("INVALID_JOIN_WINDOW");
        }
        ttlUntil(event.endAt().plus(READY_RETENTION));
    }

    private void verifyPaperPool(ExamActivatedEvent event, ExamPaperPoolDTO paperPool) {
        if (paperPool == null || paperPool.examId() == null) {
            throw new IllegalStateException("PAPER_POOL_REQUIRED");
        }
        if (!event.examId().equals(paperPool.examId())
                || event.snapshotVersion() != paperPool.snapshotVersion()) {
            throw new IllegalStateException("PAPER_POOL_SNAPSHOT_MISMATCH");
        }
    }

    private Duration ttlUntil(OffsetDateTime expireAt) {
        Duration ttl = Duration.between(OffsetDateTime.now(clock), expireAt);
        if (ttl.isZero() || ttl.isNegative()) {
            // Fail closed: activation da het han thi khong ghi READY moi vao Redis.
            throw new IllegalArgumentException("EXAM_ACTIVATION_ALREADY_EXPIRED");
        }
        return ttl;
    }

    private record PaperPoolSnapshot(ExamPaperPoolDTO paperPool, String source, String rawJson) {
    }

    public record WarmupResult(boolean processed, java.util.UUID examId, String snapshotSource) {
        static WarmupResult processed(java.util.UUID examId, String snapshotSource) {
            return new WarmupResult(true, examId, snapshotSource);
        }

        static WarmupResult duplicate(java.util.UUID examId) {
            return new WarmupResult(false, examId, null);
        }
    }
}
