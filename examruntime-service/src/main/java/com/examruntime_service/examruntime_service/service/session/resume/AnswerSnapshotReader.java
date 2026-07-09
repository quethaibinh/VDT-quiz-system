package com.examruntime_service.examruntime_service.service.session.resume;

import com.examruntime_service.examruntime_service.model.dto.session.AnswerDraftRecord;
import com.examruntime_service.examruntime_service.model.dto.session.StudentAnswerDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import com.examruntime_service.examruntime_service.repository.SessionAnswerRepo;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperMapper;
import com.examruntime_service.examruntime_service.service.session.autoSave.AnswerDraftStore;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
// Doc snapshot dap an theo thu tu Redis truoc, DB checkpoint fallback.
public class AnswerSnapshotReader {

    private final AnswerDraftStore draftStore;
    private final SessionAnswerRepo sessionAnswerRepo;
    private final StudentPaperMapper paperMapper;
    private final Clock clock;

    public AnswerSnapshotReader(
            AnswerDraftStore draftStore,
            SessionAnswerRepo sessionAnswerRepo,
            StudentPaperMapper paperMapper,
            Clock clock
    ) {
        this.draftStore = draftStore;
        this.sessionAnswerRepo = sessionAnswerRepo;
        this.paperMapper = paperMapper;
        this.clock = clock;
    }

    public List<StudentAnswerDTO> readForResume(ExamSession session, Duration ttl) {
        try {
            List<AnswerDraftRecord> redisRecords = draftStore.findAll(session.getId());
            if (!redisRecords.isEmpty()) {
                return mapDrafts(redisRecords);
            }
        } catch (Exception ignored) {
        }

        // neu redis loi thi goi xuong database, tra data cho frontend va luu lai len redis
        List<SessionAnswer> dbAnswers = sessionAnswerRepo.findAllBySessionId(session.getId());
        List<StudentAnswerDTO> result = paperMapper.mapToStudentAnswers(dbAnswers);
        hydrateRedisBestEffort(session, dbAnswers, ttl);
        return result;
    }

    // redis khong lay duoc du lieu, ham nay day data len redis sau khi fallback
    private void hydrateRedisBestEffort(ExamSession session, List<SessionAnswer> dbAnswers, Duration ttl) {
        if (dbAnswers == null || dbAnswers.isEmpty()) {
            return;
        }
        try {
            OffsetDateTime now = OffsetDateTime.now(clock);
            List<AnswerDraftRecord> records = dbAnswers.stream()
                    .map(answer -> AnswerDraftRecord.builder()
                            .questionId(answer.getQuestionId())
                            .selectedOptionIds(paperMapper.mapToStudentAnswers(List.of(answer)).getFirst().getSelectedOptionIds())
                            .answerText(answer.getAnswerText())
                            .markedForReview(answer.isMarkedForReview())
                            .clientSeq(answer.getClientSeq())
                            .serverSeq(answer.getServerSeq())
                            .serverReceivedAt(answer.getLastChangedAt())
                            .build())
                    .toList();
            draftStore.hydrate(session, records, now, ttl); // day len redis
        } catch (Exception ignored) {
        }
    }

    // map tu data answer draft redis sang data chuan de tra ve cho frontend
    private List<StudentAnswerDTO> mapDrafts(List<AnswerDraftRecord> records) {
        return records.stream()
                .map(record -> StudentAnswerDTO.builder()
                        .questionId(record.getQuestionId())
                        .selectedOptionIds(record.getSelectedOptionIds() != null ? record.getSelectedOptionIds() : List.of())
                        .answerText(record.getAnswerText())
                        .markedForReview(record.isMarkedForReview())
                        .build())
                .toList();
    }
}
