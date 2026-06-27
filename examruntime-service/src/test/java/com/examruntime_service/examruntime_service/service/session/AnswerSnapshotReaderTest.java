package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.session.AnswerDraftRecord;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import com.examruntime_service.examruntime_service.repository.SessionAnswerRepo;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperMapper;
import com.examruntime_service.examruntime_service.service.session.autoSave.AnswerDraftStore;
import com.examruntime_service.examruntime_service.service.session.resume.AnswerSnapshotReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnswerSnapshotReaderTest {

    private AnswerDraftStore draftStore;
    private SessionAnswerRepo sessionAnswerRepo;
    private AnswerSnapshotReader reader;
    private final UUID sessionId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        draftStore = mock(AnswerDraftStore.class);
        sessionAnswerRepo = mock(SessionAnswerRepo.class);
        reader = new AnswerSnapshotReader(
                draftStore,
                sessionAnswerRepo,
                new StudentPaperMapper(new ObjectMapper()),
                Clock.fixed(Instant.parse("2026-07-01T08:00:00Z"), ZoneId.of("UTC"))
        );
    }

    @Test
    void readsRedisAnswersFirst() {
        ExamSession session = session();
        when(draftStore.findAll(sessionId)).thenReturn(List.of(AnswerDraftRecord.builder()
                .questionId(questionId)
                .selectedOptionIds(List.of(optionId))
                .answerText(null)
                .markedForReview(true)
                .clientSeq(2)
                .serverSeq(3)
                .serverReceivedAt(OffsetDateTime.parse("2026-07-01T08:00:00Z"))
                .build()));

        var answers = reader.readForResume(session, Duration.ofHours(1));

        assertThat(answers).hasSize(1);
        assertThat(answers.getFirst().getQuestionId()).isEqualTo(questionId);
        assertThat(answers.getFirst().getSelectedOptionIds()).containsExactly(optionId);
        verify(sessionAnswerRepo, never()).findAllBySessionId(any());
    }

    @Test
    void fallsBackToDbCheckpointWhenRedisMisses() {
        ExamSession session = session();
        when(draftStore.findAll(sessionId)).thenReturn(List.of());
        SessionAnswer checkpoint = new SessionAnswer();
        checkpoint.setQuestionId(questionId);
        checkpoint.setSelectedOptionIds("[\"" + optionId + "\"]");
        checkpoint.setAnswerText(null);
        checkpoint.setMarkedForReview(false);
        checkpoint.setClientSeq(4);
        checkpoint.setServerSeq(5);
        checkpoint.setLastChangedAt(OffsetDateTime.parse("2026-07-01T08:00:00Z"));
        when(sessionAnswerRepo.findAllBySessionId(sessionId)).thenReturn(List.of(checkpoint));

        var answers = reader.readForResume(session, Duration.ofHours(1));

        assertThat(answers).hasSize(1);
        assertThat(answers.getFirst().getSelectedOptionIds()).containsExactly(optionId);
        verify(draftStore).hydrate(any(), any(), any(), any());
    }

    private ExamSession session() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        return session;
    }
}
