package com.examruntime_service.examruntime_service.service.activation;

import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeActivationCacheTest {

    @Test
    void buildsStableRuntimeReadinessKeys() {
        UUID eventId = UUID.fromString("8f2d65a2-c12b-4789-9a72-132d456bcdef");
        UUID examId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        assertThat(RuntimeActivationCache.inboxKey(eventId))
                .isEqualTo("runtime:inbox:exam-activated:8f2d65a2-c12b-4789-9a72-132d456bcdef");
        assertThat(RuntimeActivationCache.activationKey(examId))
                .isEqualTo("runtime:exam:00000000-0000-0000-0000-000000000000:activation");
        assertThat(RuntimeActivationCache.paperPoolKey(examId, 1))
                .isEqualTo("exam:00000000-0000-0000-0000-000000000000:v1:paper-pool");
    }

    @Test
    void acquireProcessingUsesSetNxWithTtl() {
        UUID eventId = UUID.randomUUID();
        Duration ttl = Duration.ofHours(25);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(RuntimeActivationCache.inboxKey(eventId), "PROCESSING", ttl))
                .thenReturn(true);

        RuntimeActivationCache cache = new RuntimeActivationCache(redis, JsonMapper.builder().build());

        assertThat(cache.acquireProcessing(eventId, ttl)).isTrue();
        verify(values).setIfAbsent(RuntimeActivationCache.inboxKey(eventId), "PROCESSING", ttl);
    }

    @Test
    void writesActivationMetadataAsJson() {
        UUID examId = UUID.randomUUID();
        Duration ttl = Duration.ofHours(25);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RuntimeActivationCache cache = new RuntimeActivationCache(redis, JsonMapper.builder().build());

        cache.putActivation(new RuntimeActivationMetadata(
                examId,
                1,
                OffsetDateTime.parse("2026-07-01T08:00:00+07:00"),
                OffsetDateTime.parse("2026-07-01T09:00:00+07:00"),
                15,
                10,
                RuntimeActivationMetadata.STATUS_READY,
                OffsetDateTime.parse("2026-06-24T11:00:00+07:00"),
                RuntimeActivationMetadata.SOURCE_REDIS
        ), ttl);

        verify(values).set(
                org.mockito.ArgumentMatchers.eq(RuntimeActivationCache.activationKey(examId)),
                org.mockito.ArgumentMatchers.contains("\"status\":\"READY\""),
                org.mockito.ArgumentMatchers.eq(ttl)
        );
    }
}
