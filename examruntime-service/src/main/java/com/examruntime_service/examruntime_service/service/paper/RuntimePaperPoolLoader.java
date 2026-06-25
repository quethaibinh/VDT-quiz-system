package com.examruntime_service.examruntime_service.service.paper;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

@Service
// Loader dung chung cho start/resume: Redis truoc, Exam Service fallback, luon verify snapshot.
public class RuntimePaperPoolLoader {

    private final RuntimeActivationCache activationCache;
    private final ExamServiceSnapshotClient snapshotClient;
    private final ObjectMapper objectMapper;

    public RuntimePaperPoolLoader(
            RuntimeActivationCache activationCache,
            ExamServiceSnapshotClient snapshotClient,
            ObjectMapper objectMapper
    ) {
        this.activationCache = activationCache;
        this.snapshotClient = snapshotClient;
        this.objectMapper = objectMapper;
    }

    public ExamPaperPoolDTO load(UUID examId, int snapshotVersion, Duration ttl) {
        String cached = activationCache.getPaperPool(examId, snapshotVersion);
        if (cached != null && !cached.isBlank()) {
            try {
                ExamPaperPoolDTO cachedPool = objectMapper.readValue(cached, ExamPaperPoolDTO.class);
                verifyPaperPool(examId, snapshotVersion, cachedPool);
                return cachedPool;
            } catch (Exception ignored) {
                // Cache bi loi/stale la recoverable: fallback source of truth ben Exam Service.
            }
        }

        ExamPaperPoolDTO fallback = snapshotClient.getPaperPool(examId);
        verifyPaperPool(examId, snapshotVersion, fallback);
        try {
            String payload = objectMapper.writeValueAsString(fallback);
            activationCache.putPaperPool(examId, snapshotVersion, payload, ttl);
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private void verifyPaperPool(UUID examId, int snapshotVersion, ExamPaperPoolDTO paperPool) {
        if (paperPool == null || paperPool.examId() == null) {
            throw new IllegalStateException("PAPER_POOL_REQUIRED");
        }
        if (!examId.equals(paperPool.examId()) || snapshotVersion != paperPool.snapshotVersion()) {
            throw new IllegalStateException("PAPER_POOL_SNAPSHOT_MISMATCH");
        }
    }
}
