package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.RuntimeActivationDTO;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeActivationResolverTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> values;
    private ExamServiceSnapshotClient snapshotClient;
    private RuntimeActivationCache activationCache;
    private RuntimeActivationResolver resolver;
    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> mockedValues = mock(ValueOperations.class);
        values = mockedValues;
        when(redisTemplate.opsForValue()).thenReturn(values);
        snapshotClient = mock(ExamServiceSnapshotClient.class);
        activationCache = mock(RuntimeActivationCache.class);
        resolver = new RuntimeActivationResolver(
                redisTemplate,
                JsonMapper.builder().build(),
                snapshotClient,
                activationCache,
                Clock.fixed(Instant.parse("2026-07-01T07:50:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void repairsMissingActivationMetadataFromExamService() {
        when(values.get(RuntimeActivationCache.activationKey(examId))).thenReturn(null);
        when(snapshotClient.getRuntimeActivation(examId)).thenReturn(new RuntimeActivationDTO(
                examId,
                1,
                OffsetDateTime.parse("2026-07-01T08:00:00Z"),
                OffsetDateTime.parse("2026-07-01T09:00:00Z"),
                10,
                15
        ));

        RuntimeActivationMetadata metadata = resolver.getReadyActivation(examId);

        assertThat(metadata.examId()).isEqualTo(examId);
        assertThat(metadata.status()).isEqualTo(RuntimeActivationMetadata.STATUS_READY);
        assertThat(metadata.snapshotSource()).isEqualTo(RuntimeActivationMetadata.SOURCE_EXAM_SERVICE_FALLBACK);
        verify(activationCache).putActivation(eq(metadata), any(Duration.class));
    }

    @Test
    void failsClosedWhenRepairMetadataDoesNotMatchExam() {
        when(values.get(RuntimeActivationCache.activationKey(examId))).thenReturn(null);
        when(snapshotClient.getRuntimeActivation(examId)).thenReturn(new RuntimeActivationDTO(
                UUID.randomUUID(),
                1,
                OffsetDateTime.parse("2026-07-01T08:00:00Z"),
                OffsetDateTime.parse("2026-07-01T09:00:00Z"),
                10,
                15
        ));

        assertThatThrownBy(() -> resolver.getReadyActivation(examId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("EXAM_RUNTIME_NOT_READY");
    }
}
