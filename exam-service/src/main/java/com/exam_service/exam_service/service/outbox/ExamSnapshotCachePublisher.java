package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.dto.cache.ExamAnswerKeyDTO;
import com.exam_service.exam_service.model.dto.cache.ExamPaperPoolDTO;
import com.exam_service.exam_service.model.dto.outbox.ExamSnapshotCacheRequested;
import com.exam_service.exam_service.service.exams.ExamSnapshotReadService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
/**
 * Ghi hai Redis key doc lap; retry se ghi de cung key/version nen idempotent.
 */
public class ExamSnapshotCachePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ExamSnapshotReadService snapshotReadService;

    public ExamSnapshotCachePublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ExamSnapshotReadService snapshotReadService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.snapshotReadService = snapshotReadService;
    }

    // ham xu ly tien trinh day snapshot len redis
    public void publish(ExamSnapshotCacheRequested request) {
        ExamPaperPoolDTO paper = snapshotReadService.getPaperPool(request.examId());
        ExamAnswerKeyDTO answers = snapshotReadService.getAnswerKey(request.examId());
        if (paper.snapshotVersion() != request.snapshotVersion()
                || answers.snapshotVersion() != request.snapshotVersion()) {
            throw new IllegalStateException("SNAPSHOT_VERSION_MISMATCH");
        }

        String paperKey = paperKey(request.examId(), request.snapshotVersion());
        String answerKey = answerKey(request.examId(), request.snapshotVersion());
        Instant expiresAt = request.expiresAt().toInstant();
        Duration ttl = Duration.between(Instant.now(), expiresAt); // thoi gian het han cua ban ghi redis la endAt + 24h
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("EXAM_SNAPSHOT_CACHE_EXPIRED");
        }
        // SET kem TTL la mot lenh atomic, tranh de lai answer-key khong het han.
        redisTemplate.opsForValue().set(paperKey, json(paper), ttl);
        redisTemplate.opsForValue().set(answerKey, json(answers), ttl);
    }

    public static String paperKey(UUID examId, int version) {
        return "exam:%s:v%d:paper-pool".formatted(examId, version);
    }

    public static String answerKey(UUID examId, int version) {
        return "exam:%s:v%d:answer-key".formatted(examId, version);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("CACHE_SERIALIZATION_FAILED", exception);
        }
    }
}
