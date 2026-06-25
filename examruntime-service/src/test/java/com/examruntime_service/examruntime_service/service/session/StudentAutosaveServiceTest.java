package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveRequestDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.SessionAnswerRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentAutosaveServiceTest {

    private ExamSessionRepo examSessionRepo;
    private SessionAnswerRepo sessionAnswerRepo;
    private Clock clock;
    private StudentAutosaveService autosaveService;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        examSessionRepo = mock(ExamSessionRepo.class);
        sessionAnswerRepo = mock(SessionAnswerRepo.class);
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        autosaveService = new StudentAutosaveService(
                examSessionRepo,
                sessionAnswerRepo,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void testAutosaveSuccess() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setServerDeadlineAt(OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC));
        session.setQuestionOrder("[\"" + questionId + "\"]");
        session.setOptionOrders("{\"" + questionId + "\":[\"" + optionId + "\"]}");
        session.setAutosaveSeq(1);

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(sessionAnswerRepo.findAllBySessionId(sessionId)).thenReturn(Collections.emptyList());

        AutosaveAnswerDTO ans = new AutosaveAnswerDTO(questionId, List.of(optionId), null, false);
        AutosaveRequestDTO req = new AutosaveRequestDTO(2, List.of(ans));

        var response = autosaveService.autosave(sessionId, req, studentId);

        assertThat(response.getSavedCount()).isEqualTo(1);
        assertThat(response.getSkippedCount()).isZero();
        verify(sessionAnswerRepo).save(any(SessionAnswer.class));
        verify(examSessionRepo).save(session);
    }

    @Test
    void testAutosaveSessionNotStartedThrows() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.CREATED); // Trang thai CREATED thi khong cho phep autosave

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

        AutosaveRequestDTO req = new AutosaveRequestDTO(1, Collections.emptyList());

        assertThatThrownBy(() -> autosaveService.autosave(sessionId, req, studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("SESSION_NOT_IN_PROGRESS");
    }

    @Test
    void testAutosaveStaleSequenceSkipped() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setServerDeadlineAt(OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC));
        session.setQuestionOrder("[\"" + questionId + "\"]");
        session.setOptionOrders("{\"" + questionId + "\":[\"" + optionId + "\"]}");

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

        SessionAnswer existingAnswer = new SessionAnswer();
        existingAnswer.setQuestionId(questionId);
        existingAnswer.setClientSeq(10); // Seq cu dang la 10
        when(sessionAnswerRepo.findAllBySessionId(sessionId)).thenReturn(List.of(existingAnswer));

        // Client gui den seq la 9 (stale)
        AutosaveAnswerDTO ans = new AutosaveAnswerDTO(questionId, List.of(optionId), null, false);
        AutosaveRequestDTO req = new AutosaveRequestDTO(9, List.of(ans));

        var response = autosaveService.autosave(sessionId, req, studentId);

        assertThat(response.getSavedCount()).isZero();
        assertThat(response.getSkippedCount()).isEqualTo(1);
        verify(sessionAnswerRepo, never()).save(any(SessionAnswer.class));
    }

    @Test
    void testAutosaveAfterDeadlineThrows() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setServerDeadlineAt(OffsetDateTime.ofInstant(fixedInstant.minusSeconds(1), ZoneOffset.UTC));

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

        AutosaveRequestDTO req = new AutosaveRequestDTO(1, Collections.emptyList());

        assertThatThrownBy(() -> autosaveService.autosave(sessionId, req, studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("EXAM_ALREADY_ENDED");
    }

    @Test
    void testAutosaveNullAnswersIsAcceptedAsEmptyBatch() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setQuestionOrder("[\"" + questionId + "\"]");
        session.setOptionOrders("{\"" + questionId + "\":[\"" + optionId + "\"]}");

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(sessionAnswerRepo.findAllBySessionId(sessionId)).thenReturn(Collections.emptyList());

        AutosaveRequestDTO req = new AutosaveRequestDTO(3, null);

        var response = autosaveService.autosave(sessionId, req, studentId);

        assertThat(response.getSavedCount()).isZero();
        assertThat(response.getAcceptedSeq()).isEqualTo(3);
    }

    @Test
    void testAutosaveMissingOptionOrderFailsClosed() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setQuestionOrder("[\"" + questionId + "\"]");
        session.setOptionOrders("{}");

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));

        AutosaveAnswerDTO ans = new AutosaveAnswerDTO(questionId, List.of(optionId), null, false);
        AutosaveRequestDTO req = new AutosaveRequestDTO(2, List.of(ans));

        assertThatThrownBy(() -> autosaveService.autosave(sessionId, req, studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("INVALID_OPTION_ID");
    }
}
