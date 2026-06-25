package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
// Resolver truy van thong tin ca thi va xac minh ca thi da o trang thai READY
public class RuntimeActivationResolver {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RuntimeActivationResolver(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // Lay activation metadata cua ky thi, nem ra ConflictException neu chua san sang
    public RuntimeActivationMetadata getReadyActivation(UUID examId) {
        String key = RuntimeActivationCache.activationKey(examId);
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            throw new ConflictException("EXAM_RUNTIME_NOT_READY");
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
}
