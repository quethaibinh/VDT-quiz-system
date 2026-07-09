package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.AnswerEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperOptionDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.RuntimeActivationDTO;
import com.examruntime_service.examruntime_service.model.dto.session.StudentAnswerDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.model.entity.Submission;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmissionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import com.examruntime_service.examruntime_service.repository.SubmissionRepo;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import com.examruntime_service.examruntime_service.service.paper.RuntimePaperPoolLoader;
import com.examruntime_service.examruntime_service.service.session.resume.AnswerSnapshotReader;
import com.examruntime_service.examruntime_service.service.session.submit.SubmissionFinalizationService;
import com.examruntime_service.examruntime_service.service.monitor.MonitorStateService;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionFinalizationServiceTest {

    private ExamSessionRepo examSessionRepo;
    private SubmissionRepo submissionRepo;
    private OutboxEventRepo outboxEventRepo;
    private AnswerSnapshotReader answerSnapshotReader;
    private RuntimePaperPoolLoader paperPoolLoader;
    private RuntimeActivationCache activationCache;
    private ExamServiceSnapshotClient snapshotClient;
    private MonitorStateService monitorStateService;
    private SubmissionFinalizationService service;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        examSessionRepo = mock(ExamSessionRepo.class);
        submissionRepo = mock(SubmissionRepo.class);
        outboxEventRepo = mock(OutboxEventRepo.class);
        answerSnapshotReader = mock(AnswerSnapshotReader.class);
        paperPoolLoader = mock(RuntimePaperPoolLoader.class);
        activationCache = mock(RuntimeActivationCache.class);
        snapshotClient = mock(ExamServiceSnapshotClient.class);
        monitorStateService = mock(MonitorStateService.class);
        when(paperPoolLoader.load(eq(examId), eq(3), any())).thenReturn(paperPool());
        when(snapshotClient.getAnswerKey(examId)).thenReturn(answerKey());
        service = new SubmissionFinalizationService(
                examSessionRepo,
                submissionRepo,
                outboxEventRepo,
                answerSnapshotReader,
                paperPoolLoader,
                activationCache,
                snapshotClient,
                new ObjectMapper(),
                monitorStateService,
                Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        );
    }

    @Test
    void manualSubmitCreatesSubmissionAndOutboxEvent() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(submissionRepo.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(submissionRepo.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(answerSnapshotReader.readForResume(eq(session), any())).thenReturn(List.of(answer()));
        when(submissionRepo.save(any())).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            return submission;
        });

        var response = service.finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "key-1");

        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.RECEIVED.name());
        assertThat(response.getSubmitReason()).isEqualTo(SubmitReason.STUDENT);
        assertThat(session.getStatus()).isEqualTo(ExamSessionStatus.SUBMITTED);
        assertThat(session.getSubmittedAt()).isEqualTo(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC));

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepo).save(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getAnswerCount()).isEqualTo(1);
        assertThat(submissionCaptor.getValue().getAnswerSnapshot()).contains(questionId.toString());
        assertThat(submissionCaptor.getValue().getPaperSnapshot()).contains("questionOrder", "correctOptionIds");
        assertThat(submissionCaptor.getValue().getMessageId()).isNotNull();

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepo).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("SubmissionCreated");
        assertThat(outboxCaptor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxCaptor.getValue().getPayload()).contains("\"producer\":\"examruntime-service\"");
    }

    @Test
    void submitRepairsExamSnapshotMetadataFromExamServiceWhenActivationCacheMisses() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(submissionRepo.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(submissionRepo.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(answerSnapshotReader.readForResume(eq(session), any())).thenReturn(List.of(answer()));
        when(activationCache.getActivation(examId)).thenReturn(null);
        when(snapshotClient.getRuntimeActivation(examId)).thenReturn(new RuntimeActivationDTO(
                examId,
                3,
                "EXAM-1",
                "Midterm",
                subjectId,
                "Philosophy",
                teacherId,
                OffsetDateTime.ofInstant(fixedInstant.minusSeconds(3600), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC),
                10,
                15,
                "AFTER_CLOSED"
        ));
        when(submissionRepo.save(any())).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            return submission;
        });

        service.finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "key-2");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepo).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getPayload())
                .contains("\"ownerTeacherId\":\"" + teacherId + "\"")
                .contains("\"showResultPolicy\":\"AFTER_CLOSED\"")
                .contains("\"subjectId\":\"" + subjectId + "\"");
    }

    @Test
    void duplicateSubmitReturnsExistingSubmissionForSession() {
        ExamSession session = inProgressSession();
        Submission existing = existingSubmission();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(submissionRepo.findBySessionId(sessionId)).thenReturn(Optional.of(existing));

        var response = service.finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "key-1");

        assertThat(response.getSubmissionId()).isEqualTo(existing.getId());
        verify(submissionRepo, never()).save(any());
        verify(outboxEventRepo, never()).save(any());
    }

    @Test
    void reusedIdempotencyKeyForDifferentSessionThrows() {
        ExamSession session = inProgressSession();
        Submission existing = existingSubmission();
        existing.setSessionId(UUID.randomUUID());
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(submissionRepo.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(submissionRepo.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "key-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    void timeUpSubmitMarksSessionAutoSubmitted() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(submissionRepo.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(answerSnapshotReader.readForResume(eq(session), any())).thenReturn(List.of());
        when(submissionRepo.save(any())).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            return submission;
        });

        var response = service.finalizeSubmission(sessionId, null, SubmitReason.TIME_UP, null);

        assertThat(response.getSubmitReason()).isEqualTo(SubmitReason.TIME_UP);
        assertThat(session.getStatus()).isEqualTo(ExamSessionStatus.AUTO_SUBMITTED);
        verify(submissionRepo, never()).findByIdempotencyKey(anyString());
    }

    @Test
    void lateManualSubmitIsRecordedAsTimeUp() {
        ExamSession session = inProgressSession();
        session.setServerDeadlineAt(OffsetDateTime.ofInstant(fixedInstant.minusSeconds(1), ZoneOffset.UTC));
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(submissionRepo.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(answerSnapshotReader.readForResume(eq(session), any())).thenReturn(List.of());
        when(submissionRepo.save(any())).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            return submission;
        });

        var response = service.finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "late-key");

        assertThat(response.getSubmitReason()).isEqualTo(SubmitReason.TIME_UP);
        assertThat(session.getStatus()).isEqualTo(ExamSessionStatus.AUTO_SUBMITTED);
        assertThat(session.getSubmittedAt()).isEqualTo(session.getServerDeadlineAt());
    }

    private ExamSession inProgressSession() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setAttemptNo(1);
        session.setSnapshotVersion(3);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setServerDeadlineAt(OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC));
        session.setQuestionOrder("[\"" + questionId + "\"]");
        session.setOptionOrders("{\"" + questionId + "\":[\"" + optionId + "\"]}");
        return session;
    }

    private StudentAnswerDTO answer() {
        return StudentAnswerDTO.builder()
                .questionId(questionId)
                .selectedOptionIds(List.of(optionId))
                .markedForReview(false)
                .build();
    }

    private ExamPaperPoolDTO paperPool() {
        return new ExamPaperPoolDTO(
                examId,
                3,
                1,
                0,
                0,
                List.of(new PaperQuestionDTO(
                        questionId,
                        1,
                        "EASY",
                        "SINGLE_CHOICE",
                        "Question",
                        "TEXT",
                        1.0,
                        List.of(new PaperOptionDTO(optionId, "A", "Answer", "TEXT"))
                ))
        );
    }

    private ExamAnswerKeyDTO answerKey() {
        return new ExamAnswerKeyDTO(
                examId,
                3,
                List.of(new AnswerEntryDTO(questionId, List.of(optionId), 1.0))
        );
    }

    private Submission existingSubmission() {
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setSessionId(sessionId);
        submission.setStatus(SubmissionStatus.RECEIVED);
        submission.setSubmitReason(SubmitReason.STUDENT);
        submission.setSubmittedAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC));
        return submission;
    }
}
