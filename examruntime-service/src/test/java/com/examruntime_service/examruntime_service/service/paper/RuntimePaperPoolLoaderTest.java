package com.examruntime_service.examruntime_service.service.paper;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimePaperPoolLoaderTest {

    private RuntimeActivationCache activationCache;
    private ExamServiceSnapshotClient snapshotClient;
    private RuntimePaperPoolLoader loader;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        activationCache = mock(RuntimeActivationCache.class);
        snapshotClient = mock(ExamServiceSnapshotClient.class);
        objectMapper = new ObjectMapper();
        loader = new RuntimePaperPoolLoader(activationCache, snapshotClient, objectMapper);
    }

    @Test
    void testValidCacheHitDoesNotCallFallback() throws Exception {
        UUID examId = UUID.randomUUID();
        ExamPaperPoolDTO pool = new ExamPaperPoolDTO(examId, 1, 0, 0, 0, List.of());
        when(activationCache.getPaperPool(examId, 1)).thenReturn(objectMapper.writeValueAsString(pool));

        ExamPaperPoolDTO result = loader.load(examId, 1, Duration.ofHours(1));

        assertThat(result).isEqualTo(pool);
        verify(snapshotClient, never()).getPaperPool(any());
    }

    @Test
    void testMismatchedFallbackFailsClosed() {
        UUID examId = UUID.randomUUID();
        when(activationCache.getPaperPool(examId, 1)).thenReturn(null);
        when(snapshotClient.getPaperPool(examId))
                .thenReturn(new ExamPaperPoolDTO(UUID.randomUUID(), 1, 0, 0, 0, List.of()));

        assertThatThrownBy(() -> loader.load(examId, 1, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PAPER_POOL_SNAPSHOT_MISMATCH");
    }
}
