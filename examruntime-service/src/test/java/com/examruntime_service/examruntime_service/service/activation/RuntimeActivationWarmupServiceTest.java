package com.examruntime_service.examruntime_service.service.activation;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.events.ExamActivatedEvent;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeActivationWarmupServiceTest {

    private ObjectMapper objectMapper;
    private RuntimeActivationCache cache;
    private ExamServiceSnapshotClient snapshotClient;
    private RuntimeActivationWarmupService service;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        cache = mock(RuntimeActivationCache.class);
        snapshotClient = mock(ExamServiceSnapshotClient.class);
        service = new RuntimeActivationWarmupService(
                objectMapper,
                cache,
                snapshotClient,
                Clock.fixed(Instant.parse("2026-06-24T04:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void existingPaperPoolWritesReadyMetadataWithRedisSource() throws Exception {
        ExamActivatedEvent event = event();
        ExamPaperPoolDTO paperPool = new ExamPaperPoolDTO(event.examId(), event.snapshotVersion(), 1, 0, 0, List.of());
        when(cache.acquireProcessing(eq(event.eventId()), any())).thenReturn(true);
        when(cache.getPaperPool(event.examId(), event.snapshotVersion()))
                .thenReturn(objectMapper.writeValueAsString(paperPool));

        RuntimeActivationWarmupService.WarmupResult result = service.handle(objectMapper.writeValueAsString(event));

        assertThat(result.processed()).isTrue();
        assertThat(result.snapshotSource()).isEqualTo(RuntimeActivationMetadata.SOURCE_REDIS);
        verify(snapshotClient, never()).getPaperPool(any());
        verify(cache).putActivation(org.mockito.ArgumentMatchers.argThat(metadata ->
                metadata.examId().equals(event.examId())
                        && metadata.status().equals(RuntimeActivationMetadata.STATUS_READY)
                        && metadata.snapshotSource().equals(RuntimeActivationMetadata.SOURCE_REDIS)
        ), any());
        verify(cache).markProcessed(eq(event.eventId()), any());
    }

    @Test
    void missingPaperPoolHydratesFromExamServiceBeforeReady() throws Exception {
        ExamActivatedEvent event = event();
        ExamPaperPoolDTO paperPool = new ExamPaperPoolDTO(event.examId(), event.snapshotVersion(), 1, 0, 0, List.of());
        when(cache.acquireProcessing(eq(event.eventId()), any())).thenReturn(true);
        when(cache.getPaperPool(event.examId(), event.snapshotVersion())).thenReturn(null);
        when(snapshotClient.getPaperPool(event.examId())).thenReturn(paperPool);

        RuntimeActivationWarmupService.WarmupResult result = service.handle(objectMapper.writeValueAsString(event));

        assertThat(result.snapshotSource()).isEqualTo(RuntimeActivationMetadata.SOURCE_EXAM_SERVICE_FALLBACK);
        verify(cache).putPaperPool(
                eq(event.examId()),
                eq(event.snapshotVersion()),
                org.mockito.ArgumentMatchers.contains("\"snapshotVersion\":1"),
                any()
        );
        verify(cache).putActivation(org.mockito.ArgumentMatchers.argThat(metadata ->
                metadata.snapshotSource().equals(RuntimeActivationMetadata.SOURCE_EXAM_SERVICE_FALLBACK)
        ), any());
    }

    @Test
    void duplicateEventSkipsWarmup() throws Exception {
        ExamActivatedEvent event = event();
        when(cache.acquireProcessing(eq(event.eventId()), any())).thenReturn(false);

        RuntimeActivationWarmupService.WarmupResult result = service.handle(objectMapper.writeValueAsString(event));

        assertThat(result.processed()).isFalse();
        verify(cache, never()).getPaperPool(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(snapshotClient, never()).getPaperPool(any());
        verify(cache, never()).putActivation(any(), any());
    }

    @Test
    void mismatchedFallbackDoesNotWriteReadyAndReleasesInbox() throws Exception {
        ExamActivatedEvent event = event();
        when(cache.acquireProcessing(eq(event.eventId()), any())).thenReturn(true);
        when(cache.getPaperPool(event.examId(), event.snapshotVersion())).thenReturn(null);
        when(snapshotClient.getPaperPool(event.examId()))
                .thenReturn(new ExamPaperPoolDTO(UUID.randomUUID(), event.snapshotVersion(), 1, 0, 0, List.of()));

        assertThatThrownBy(() -> service.handle(objectMapper.writeValueAsString(event)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PAPER_POOL_SNAPSHOT_MISMATCH");

        verify(cache, never()).putActivation(any(), any());
        verify(cache).deleteInbox(event.eventId());
    }

    @Test
    void expiredEventFailsBeforeInboxAcquire() throws Exception {
        ExamActivatedEvent event = new ExamActivatedEvent(
                UUID.randomUUID(),
                ExamActivatedEvent.EVENT_TYPE,
                ExamActivatedEvent.EVENT_VERSION,
                UUID.randomUUID(),
                1,
                OffsetDateTime.parse("2026-06-22T08:00:00+07:00"),
                OffsetDateTime.parse("2026-06-22T09:00:00+07:00"),
                15,
                10,
                OffsetDateTime.parse("2026-06-22T07:00:00+07:00")
        );

        assertThatThrownBy(() -> service.handle(objectMapper.writeValueAsString(event)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EXAM_ACTIVATION_ALREADY_EXPIRED");

        verify(cache, never()).acquireProcessing(any(), any());
    }

    private ExamActivatedEvent event() {
        return new ExamActivatedEvent(
                UUID.randomUUID(),
                ExamActivatedEvent.EVENT_TYPE,
                ExamActivatedEvent.EVENT_VERSION,
                UUID.randomUUID(),
                1,
                OffsetDateTime.parse("2026-07-01T08:00:00+07:00"),
                OffsetDateTime.parse("2026-07-01T09:00:00+07:00"),
                15,
                10,
                OffsetDateTime.parse("2026-06-24T11:00:00+07:00")
        );
    }
}
