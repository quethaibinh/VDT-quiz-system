package com.examruntime_service.examruntime_service.service.session.autoSave;

import com.examruntime_service.examruntime_service.model.dto.session.AnswerDraftRecord;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.SessionAnswerRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
// Ghi checkpoint ben vung xuong PostgreSQL cho fallback va audit.
public class SessionAnswerCheckpointWriter {

    private final SessionAnswerRepo sessionAnswerRepo;
    private final ExamSessionRepo examSessionRepo;
    private final ObjectMapper objectMapper;

    public SessionAnswerCheckpointWriter(
            SessionAnswerRepo sessionAnswerRepo,
            ExamSessionRepo examSessionRepo,
            ObjectMapper objectMapper
    ) {
        this.sessionAnswerRepo = sessionAnswerRepo;
        this.examSessionRepo = examSessionRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnswerDraftSaveResult writeAutosaveFallback(
            ExamSession session,
            List<AutosaveAnswerDTO> answers,
            long clientSeq,
            OffsetDateTime now
    ) {
        long serverSeq = session.getAutosaveSeq() + 1;
        int savedCount = 0;
        int skippedCount = 0;

        List<SessionAnswer> existingList = sessionAnswerRepo.findAllBySessionId(session.getId());
        Map<UUID, SessionAnswer> existingMap = existingList.stream()
                .collect(Collectors.toMap(SessionAnswer::getQuestionId, Function.identity()));

        for (AutosaveAnswerDTO answer : answers) {
            SessionAnswer existing = existingMap.get(answer.getQuestionId());
            if (existing != null && existing.getClientSeq() > clientSeq) {
                skippedCount++;
                continue;
            }
            upsert(session, answer.getQuestionId(), answer.getSelectedOptionIds(), answer.getAnswerText(),
                    answer.isMarkedForReview(), clientSeq, serverSeq, now, existing);
            savedCount++;
        }

        session.setAutosaveSeq(serverSeq);
        session.setLastAutosaveAt(now);
        int answeredCount = countAnswered(session.getId());
        session.setAnsweredCount(answeredCount);
        examSessionRepo.save(session);

        return AnswerDraftSaveResult.builder()
                .savedCount(savedCount)
                .skippedCount(skippedCount)
                .answeredCount(answeredCount)
                .serverSeq(serverSeq)
                .lastAutosaveAt(now)
                .build();
    }

    @Transactional
    public void writeCheckpoint(ExamSession session, List<AnswerDraftRecord> records, OffsetDateTime now) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<SessionAnswer> existingList = sessionAnswerRepo.findAllBySessionId(session.getId());
        Map<UUID, SessionAnswer> existingMap = existingList.stream()
                .collect(Collectors.toMap(SessionAnswer::getQuestionId, Function.identity()));

        long maxServerSeq = session.getAutosaveSeq();
        for (AnswerDraftRecord record : records) {
            SessionAnswer existing = existingMap.get(record.getQuestionId());
            if (existing != null && existing.getClientSeq() > record.getClientSeq()) {
                continue;
            }
            maxServerSeq = Math.max(maxServerSeq, record.getServerSeq());
            upsert(session, record.getQuestionId(), record.getSelectedOptionIds(), record.getAnswerText(),
                    record.isMarkedForReview(), record.getClientSeq(), record.getServerSeq(),
                    record.getServerReceivedAt() != null ? record.getServerReceivedAt() : now, existing);
        }

        session.setAutosaveSeq(maxServerSeq);
        session.setLastAutosaveAt(now);
        session.setAnsweredCount(countAnswered(session.getId()));
        examSessionRepo.save(session);
    }

    private void upsert(
            ExamSession session,
            UUID questionId,
            List<UUID> selectedOptionIds,
            String answerText,
            boolean markedForReview,
            long clientSeq,
            long serverSeq,
            OffsetDateTime changedAt,
            SessionAnswer existing
    ) {
        SessionAnswer target = existing != null ? existing : new SessionAnswer();
        if (existing == null) {
            target.setSessionId(session.getId());
            target.setExamId(session.getExamId());
            target.setStudentId(session.getStudentId());
            target.setQuestionId(questionId);
            target.setAnsweredAt(changedAt);
        }
        target.setSelectedOptionIds(serializeOptions(selectedOptionIds));
        target.setAnswerText(answerText);
        target.setMarkedForReview(markedForReview);
        target.setClientSeq(clientSeq);
        target.setServerSeq(serverSeq);
        target.setLastChangedAt(changedAt);
        sessionAnswerRepo.save(target);
    }

    private int countAnswered(UUID sessionId) {
        return (int) sessionAnswerRepo.findAllBySessionId(sessionId).stream()
                .filter(answer -> (answer.getSelectedOptionIds() != null
                        && !answer.getSelectedOptionIds().isBlank()
                        && !answer.getSelectedOptionIds().equals("[]"))
                        || (answer.getAnswerText() != null && !answer.getAnswerText().isBlank()))
                .count();
    }

    private String serializeOptions(List<UUID> selectedOptionIds) {
        try {
            return objectMapper.writeValueAsString(selectedOptionIds != null ? selectedOptionIds : List.of());
        } catch (Exception exception) {
            throw new IllegalStateException("SERIALIZATION_FAILED", exception);
        }
    }
}
