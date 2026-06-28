package com.examruntime_service.examruntime_service.service.session.autoSave;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
// Ket qua ghi draft answer vao Redis.
public record AnswerDraftSaveResult(
        int savedCount,
        int skippedCount,
        long serverSeq,
        OffsetDateTime lastAutosaveAt
) {
}
