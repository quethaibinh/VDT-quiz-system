package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.RuntimeActivationDTO;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
// Resolver truy van thong tin ca thi va xac minh ca thi da o trang thai READY
public class RuntimeActivationResolver {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ExamServiceSnapshotClient snapshotClient;
    private final RuntimeActivationCache activationCache;
    private final Clock clock;

    public RuntimeActivationResolver(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ExamServiceSnapshotClient snapshotClient,
            RuntimeActivationCache activationCache,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.snapshotClient = snapshotClient;
        this.activationCache = activationCache;
        this.clock = clock;
    }

    // Lay activation metadata cua ky thi, nem ra ConflictException neu chua san sang
    public RuntimeActivationMetadata getReadyActivation(UUID examId) {
        String key = RuntimeActivationCache.activationKey(examId);
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return repairFromExamService(examId);
        }

        try {
            RuntimeActivationMetadata metadata = objectMapper.readValue(payload, RuntimeActivationMetadata.class);
            if (!RuntimeActivationMetadata.STATUS_READY.equals(metadata.status())) {
                throw new ConflictException("EXAM_RUNTIME_NOT_READY");
            }
            return metadata;
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new ConflictException("EXAM_RUNTIME_NOT_READY");
        }
    }

    private RuntimeActivationMetadata repairFromExamService(UUID examId) {
        try {
            RuntimeActivationDTO dto = snapshotClient.getRuntimeActivation(examId);
            RuntimeActivationMetadata metadata = toReadyMetadata(examId, dto);
            activationCache.putActivation(metadata, ttl(metadata));
            return metadata;
        } catch (ConflictException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ConflictException("EXAM_RUNTIME_NOT_READY");
        }
    }

    private RuntimeActivationMetadata toReadyMetadata(UUID examId, RuntimeActivationDTO dto) {
        if (dto == null
                || !examId.equals(dto.examId())
                || dto.snapshotVersion() <= 0
                || dto.startAt() == null
                || dto.endAt() == null
                || !dto.endAt().isAfter(dto.startAt())
                || dto.joinBeforeMinutes() < 0
                || dto.joinAfterMinutes() < 0) {
            throw new ConflictException("EXAM_RUNTIME_NOT_READY");
        }
        return new RuntimeActivationMetadata(
                dto.examId(),
                dto.snapshotVersion(),
                dto.code(),
                dto.title(),
                dto.subjectId(),
                dto.subjectName(),
                dto.ownerTeacherId(),
                dto.startAt(),
                dto.endAt(),
                dto.joinBeforeMinutes(),
                dto.joinAfterMinutes(),
                dto.showResultPolicy(),
                RuntimeActivationMetadata.STATUS_READY,
                OffsetDateTime.now(clock),
                RuntimeActivationMetadata.SOURCE_EXAM_SERVICE_FALLBACK
        );
    }

    private Duration ttl(RuntimeActivationMetadata metadata) {
        Duration ttl = Duration.between(OffsetDateTime.now(clock), metadata.endAt().plusHours(24));
        if (ttl.isZero() || ttl.isNegative()) {
            throw new ConflictException("EXAM_RUNTIME_NOT_READY");
        }
        return ttl;
    }
}
