package com.examruntime_service.examruntime_service.service.activation;

import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

@Component
/**
 * Gom cac thao tac Redis va key naming cho activation readiness.
 * Redis o day la cache/inbox MVP, khong phai audit log ben vung.
 */
public class RuntimeActivationCache {

    public static final String TOPIC_DEFAULT = "exam-lifecycle-events";
    public static final String GROUP_DEFAULT = "examruntime-service";
    public static final String INBOX_PROCESSING = "PROCESSING";
    public static final String INBOX_PROCESSED = "PROCESSED";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RuntimeActivationCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean acquireProcessing(UUID eventId, Duration ttl) {
        // SET NX dam bao chi mot instance xu ly eventId tai mot thoi diem.
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(inboxKey(eventId), INBOX_PROCESSING, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public String getPaperPool(UUID examId, int snapshotVersion) {
        return redisTemplate.opsForValue().get(paperPoolKey(examId, snapshotVersion));
    }

    public void putPaperPool(UUID examId, int snapshotVersion, String payload, Duration ttl) {
        redisTemplate.opsForValue().set(paperPoolKey(examId, snapshotVersion), payload, ttl);
    }

    public String getAnswerKey(UUID examId, int snapshotVersion) {
        return redisTemplate.opsForValue().get(answerKeyKey(examId, snapshotVersion));
    }

    public void putAnswerKey(UUID examId, int snapshotVersion, String payload, Duration ttl) {
        redisTemplate.opsForValue().set(answerKeyKey(examId, snapshotVersion), payload, ttl);
    }

    public void putActivation(RuntimeActivationMetadata metadata, Duration ttl) {
        try {
            // Metadata nay la read path sau nay cho join-window enforcement.
            String payload = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(activationKey(metadata.examId()), payload, ttl);
        } catch (Exception exception) {
            throw new IllegalStateException("RUNTIME_ACTIVATION_METADATA_SERIALIZATION_FAILED", exception);
        }
    }

    public RuntimeActivationMetadata getActivation(UUID examId) {
        String payload = redisTemplate.opsForValue().get(activationKey(examId));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, RuntimeActivationMetadata.class);
        } catch (Exception exception) {
            return null;
        }
    }

    public void markProcessed(UUID eventId, Duration ttl) {
        redisTemplate.opsForValue().set(inboxKey(eventId), INBOX_PROCESSED, ttl);
    }

    public void deleteInbox(UUID eventId) {
        redisTemplate.delete(inboxKey(eventId));
    }

    public static String inboxKey(UUID eventId) {
        return "runtime:inbox:exam-activated:%s".formatted(eventId);
    }

    public static String activationKey(UUID examId) {
        return "runtime:exam:%s:activation".formatted(examId);
    }

    public static String paperPoolKey(UUID examId, int snapshotVersion) {
        return "exam:%s:v%d:paper-pool".formatted(examId, snapshotVersion);
    }

    public static String answerKeyKey(UUID examId, int snapshotVersion) {
        return "exam:%s:v%d:answer-key".formatted(examId, snapshotVersion);
    }
}
