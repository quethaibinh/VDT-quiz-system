package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.client.ExamServiceAssignmentClient;
import com.examruntime_service.examruntime_service.model.dto.cache.InternalExamAssignmentDTO;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
// Resolver kiem tra quyen tham gia thi va lay ra assignmentId tuong ung cua student
public class RuntimeAssignmentResolver {

    private final StringRedisTemplate redisTemplate;
    private final ExamServiceAssignmentClient assignmentClient;
    private final ObjectMapper objectMapper;

    public RuntimeAssignmentResolver(
            StringRedisTemplate redisTemplate,
            ExamServiceAssignmentClient assignmentClient,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.assignmentClient = assignmentClient;
        this.objectMapper = objectMapper;
    }

    // Resolve assignmentId tu examId va studentId, ho tro cache-aside
    public UUID resolveAssignmentId(UUID examId, UUID studentId) {
        String setKey = "exam:%s:students".formatted(examId);
        String hashKey = "exam:%s:student-assignments".formatted(examId);

        // 1. Kiem tra Set chua studentId ton tai trong Redis
        Boolean hasKey = redisTemplate.hasKey(setKey);
        if (Boolean.TRUE.equals(hasKey)) {
            Boolean isMember = redisTemplate.opsForSet().isMember(setKey, studentId.toString());
            if (Boolean.TRUE.equals(isMember)) {
                String assignmentIdStr = (String) redisTemplate.opsForHash().get(hashKey, studentId.toString());
                if (assignmentIdStr != null) {
                    return UUID.fromString(assignmentIdStr);
                }
                // Set co student nhung hash mat value: cache partial, fallback de hydrate lai.
            } else {
                return null;
            }
        }

        // 2. Cache miss: goi API noi bo cua exam-service lam fallback
        InternalExamAssignmentDTO fallbackData;
        try {
            fallbackData = assignmentClient.getAssignments(examId);
        } catch (Exception e) {
            return null;
        }

        if (fallbackData == null) {
            return null;
        }

        Duration ttl = getTtlFromMetadata(examId);

        // Xoa va tai thiet lap cache
        redisTemplate.delete(setKey);
        redisTemplate.delete(hashKey);

        if (fallbackData.getAssignments() != null && !fallbackData.getAssignments().isEmpty()) {
            String[] studentIds = fallbackData.getAssignments().stream()
                    .map(d -> d.getStudentId().toString())
                    .toArray(String[]::new);
            redisTemplate.opsForSet().add(setKey, studentIds);

            Map<String, String> map = fallbackData.getAssignments().stream()
                    .collect(Collectors.toMap(
                            d -> d.getStudentId().toString(),
                            d -> d.getAssignmentId().toString()
                    ));
            redisTemplate.opsForHash().putAll(hashKey, map);

            redisTemplate.expire(setKey, ttl);
            redisTemplate.expire(hashKey, ttl);

            if (map.containsKey(studentId.toString())) {
                return UUID.fromString(map.get(studentId.toString()));
            }
        }

        return null;
    }

    private Duration getTtlFromMetadata(UUID examId) {
        String payload = redisTemplate.opsForValue().get(RuntimeActivationCache.activationKey(examId));
        if (payload != null) {
            try {
                RuntimeActivationMetadata metadata = objectMapper.readValue(payload, RuntimeActivationMetadata.class);
                if (metadata.endAt() != null) {
                    Instant expiresAt = metadata.endAt().plusHours(24).toInstant();
                    Duration ttl = Duration.between(Instant.now(), expiresAt);
                    if (!ttl.isNegative() && !ttl.isZero()) {
                        return ttl;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return Duration.ofHours(24);
    }
}
