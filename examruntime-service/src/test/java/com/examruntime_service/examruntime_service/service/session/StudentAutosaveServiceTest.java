package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveRequestDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.service.session.autoSave.AnswerDraftStore;
import com.examruntime_service.examruntime_service.service.session.autoSave.SessionAnswerCheckpointWriter;
import com.examruntime_service.examruntime_service.service.session.autoSave.AnswerDraftSaveResult;
import com.examruntime_service.examruntime_service.service.monitor.MonitorStateService;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentAutosaveServiceTest {

    private ExamSessionRepo examSessionRepo;
    private AnswerDraftStore answerDraftStore;
    private SessionAnswerCheckpointWriter checkpointWriter;
    private MonitorStateService monitorStateService;
    private Clock clock;
    private StudentAutosaveService autosaveService;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        examSessionRepo = mock(ExamSessionRepo.class);
        answerDraftStore = mock(AnswerDraftStore.class);
        checkpointWriter = mock(SessionAnswerCheckpointWriter.class);
        monitorStateService = mock(MonitorStateService.class);
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        autosaveService = new StudentAutosaveService(
                examSessionRepo,
                answerDraftStore,
                checkpointWriter,
                new ObjectMapper(),
                monitorStateService,
                clock
        );
    }

    @Test
    void autosaveWritesRedisWhenHealthy() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(answerDraftStore.saveBatch(eq(session), any(), eq(2L), any(), any(Duration.class)))
                .thenReturn(AnswerDraftSaveResult.builder()
                        .savedCount(1)
                        .skippedCount(0)
                        .answeredCount(1)
                        .serverSeq(2)
                        .lastAutosaveAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC))
                        .build());

        AutosaveRequestDTO req = new AutosaveRequestDTO(2, List.of(answer()));

        var response = autosaveService.autosave(sessionId, req, studentId);

        assertThat(response.getSavedCount()).isEqualTo(1);
        assertThat(response.getSkippedCount()).isZero();
        assertThat(response.getStoreMode()).isEqualTo("REDIS");
        verify(answerDraftStore).saveBatch(eq(session), any(), eq(2L), any(), any(Duration.class));
        verify(checkpointWriter, never()).writeAutosaveFallback(any(), any(), any(Long.class), any());
        verify(monitorStateService).updateAnsweredCount(
                eq(session),
                eq(1),
                eq(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC))
        );
    }

    @Test
    void autosaveFallsBackToDbWhenRedisFails() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(answerDraftStore.saveBatch(eq(session), any(), eq(2L), any(), any(Duration.class)))
                .thenThrow(new IllegalStateException("redis down"));
        when(checkpointWriter.writeAutosaveFallback(eq(session), any(), eq(2L), any()))
                .thenReturn(AnswerDraftSaveResult.builder()
                        .savedCount(1)
                        .skippedCount(0)
                        .answeredCount(1)
                        .serverSeq(2)
                        .lastAutosaveAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC))
                        .build());

        var response = autosaveService.autosave(sessionId, new AutosaveRequestDTO(2, List.of(answer())), studentId);

        assertThat(response.getStoreMode()).isEqualTo("DB_FALLBACK");
        assertThat(response.getSavedCount()).isEqualTo(1);
        verify(checkpointWriter).writeAutosaveFallback(eq(session), any(), eq(2L), any());
    }

    @Test
    void autosaveSessionNotStartedThrows() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.CREATED);

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> autosaveService.autosave(sessionId, new AutosaveRequestDTO(1, Collections.emptyList()), studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("SESSION_NOT_IN_PROGRESS");
    }

    @Test
    void staleSequenceResultFromStoreIsReturned() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(answerDraftStore.saveBatch(eq(session), any(), eq(9L), any(), any(Duration.class)))
                .thenReturn(AnswerDraftSaveResult.builder()
                        .savedCount(0)
                        .skippedCount(1)
                        .answeredCount(1)
                        .serverSeq(11)
                        .lastAutosaveAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC))
                        .build());

        var response = autosaveService.autosave(sessionId, new AutosaveRequestDTO(9, List.of(answer())), studentId);

        assertThat(response.getSavedCount()).isZero();
        assertThat(response.getSkippedCount()).isEqualTo(1);
    }

    @Test
    void autosaveAfterDeadlineThrows() {
        ExamSession session = inProgressSession();
        session.setServerDeadlineAt(OffsetDateTime.ofInstant(fixedInstant.minusSeconds(1), ZoneOffset.UTC));

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> autosaveService.autosave(sessionId, new AutosaveRequestDTO(1, Collections.emptyList()), studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("EXAM_ALREADY_ENDED");
    }

    @Test
    void nullAnswersIsAcceptedAsEmptyBatch() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(answerDraftStore.saveBatch(eq(session), eq(Collections.emptyList()), eq(3L), any(), any(Duration.class)))
                .thenReturn(AnswerDraftSaveResult.builder()
                        .savedCount(0)
                        .skippedCount(0)
                        .answeredCount(0)
                        .serverSeq(2)
                        .lastAutosaveAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC))
                        .build());

        var response = autosaveService.autosave(sessionId, new AutosaveRequestDTO(3, null), studentId);

        assertThat(response.getSavedCount()).isZero();
        assertThat(response.getAcceptedSeq()).isEqualTo(3);
        assertThat(response.getStoreMode()).isEqualTo("REDIS");
    }

    @Test
    void missingOptionOrderFailsClosed() {
        ExamSession session = inProgressSession();
        session.setOptionOrders("{}");

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> autosaveService.autosave(sessionId, new AutosaveRequestDTO(2, List.of(answer())), studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("INVALID_OPTION_ID");
    }

    private ExamSession inProgressSession() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setServerDeadlineAt(OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC));
        session.setQuestionOrder("[\"" + questionId + "\"]");
        session.setOptionOrders("{\"" + questionId + "\":[\"" + optionId + "\"]}");
        session.setAutosaveSeq(1);
        return session;
    }

    private AutosaveAnswerDTO answer() {
        return new AutosaveAnswerDTO(questionId, List.of(optionId), null, false);
    }
}
