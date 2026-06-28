package com.examruntime_service.examruntime_service.service.session.autoSave;

import com.examruntime_service.examruntime_service.model.dto.session.AnswerDraftRecord;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AnswerDraftStore {

    AnswerDraftSaveResult saveBatch(
            ExamSession session,
            List<AutosaveAnswerDTO> answers,
            long clientSeq,
            OffsetDateTime now,
            Duration ttl
    );

    List<AnswerDraftRecord> findAll(UUID sessionId);

    void hydrate(ExamSession session, List<AnswerDraftRecord> records, OffsetDateTime now, Duration ttl);

    Set<UUID> findDirtySessionIds();

    void clearDirtySession(UUID sessionId);
}
