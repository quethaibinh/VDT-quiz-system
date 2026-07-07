package com.examruntime_service.examruntime_service.service.session.autoSave;

import com.examruntime_service.examruntime_service.model.dto.session.AnswerDraftRecord;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "examruntime.answers.checkpoint.enabled",
        havingValue = "true",
        matchIfMissing = true
)
// Worker dinh ky flush Redis draft answer xuong DB checkpoint.
public class AnswerCheckpointWorker {

    private static final Logger log = LoggerFactory.getLogger(AnswerCheckpointWorker.class);

    private final AnswerDraftStore draftStore;
    private final ExamSessionRepo examSessionRepo;
    private final SessionAnswerCheckpointWriter checkpointWriter;
    private final Clock clock;

    public AnswerCheckpointWorker(
            AnswerDraftStore draftStore,
            ExamSessionRepo examSessionRepo,
            SessionAnswerCheckpointWriter checkpointWriter,
            Clock clock
    ) {
        this.draftStore = draftStore;
        this.examSessionRepo = examSessionRepo;
        this.checkpointWriter = checkpointWriter;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${examruntime.answers.checkpoint.fixed-delay-ms:10000}")
    public void flushDirtySessions() {
        try {
            for (UUID sessionId : draftStore.findDirtySessionIds()) {
                try {
                    flushOne(sessionId);
                } catch (Exception exception) {
                    log.warn("Answer checkpoint failed for sessionId={}", sessionId, exception);
                }
            }
        } catch (Exception exception) {
            log.warn("Cannot read dirty answer sessions from Redis; checkpoint will retry later: {}", exception.getMessage());
        }
    }

    public void flushOne(UUID sessionId) {
        ExamSession session = examSessionRepo.findById(sessionId).orElse(null);
        if (session == null) {
            draftStore.clearDirtySession(sessionId);
            return;
        }

        List<AnswerDraftRecord> records = draftStore.findAll(sessionId);
        if (records.isEmpty()) {
            draftStore.clearDirtySession(sessionId);
            return;
        }

        checkpointWriter.writeCheckpoint(session, records, OffsetDateTime.now(clock));
        draftStore.clearDirtySession(sessionId);
    }
}
