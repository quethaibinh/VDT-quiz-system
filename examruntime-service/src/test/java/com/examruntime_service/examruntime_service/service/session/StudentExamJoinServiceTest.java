package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentExamJoinServiceTest {

    private RuntimeActivationResolver activationResolver;
    private RuntimeAssignmentResolver assignmentResolver;
    private ExamSessionRepo examSessionRepo;
    private Clock clock;
    private StudentExamJoinService joinService;

    private final UUID examId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        activationResolver = mock(RuntimeActivationResolver.class);
        assignmentResolver = mock(RuntimeAssignmentResolver.class);
        examSessionRepo = mock(ExamSessionRepo.class);
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        joinService = new StudentExamJoinService(
                activationResolver,
                assignmentResolver,
                examSessionRepo,
                clock
        );
    }

    @Test
    void testJoinSuccess() {
        // startAt: sau 5 phut, endAt: sau 1 gio
        RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                examId,
                1,
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(300), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC),
                10,
                10,
                "READY",
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                "REDIS"
        );

        when(activationResolver.getReadyActivation(examId)).thenReturn(metadata);
        when(assignmentResolver.resolveAssignmentId(examId, studentId)).thenReturn(assignmentId);

        ExamSession session = new ExamSession();
        session.setId(UUID.randomUUID());
        session.setStatus(ExamSessionStatus.CREATED);

        when(examSessionRepo.findByExamIdAndStudentIdAndAttemptNo(examId, studentId, 1)).thenReturn(Optional.empty());
        when(examSessionRepo.saveAndFlush(any())).thenReturn(session);

        var response = joinService.joinExam(examId, studentId);

        assertThat(response.getSessionId()).isEqualTo(session.getId());
        assertThat(response.getStatus()).isEqualTo(ExamSessionStatus.CREATED);
        assertThat(response.isCanStart()).isFalse();
    }

    @Test
    void testJoinTooEarlyThrows() {
        // startAt: sau 20 phut, joinBeforeMinutes la 10 phut -> qua som de join
        RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                examId,
                1,
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(1200), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC),
                10,
                10,
                "READY",
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                "REDIS"
        );

        when(activationResolver.getReadyActivation(examId)).thenReturn(metadata);
        when(assignmentResolver.resolveAssignmentId(examId, studentId)).thenReturn(assignmentId);

        assertThatThrownBy(() -> joinService.joinExam(examId, studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("EXAM_JOIN_NOT_OPEN");
    }

    @Test
    void testJoinTooLateThrows() {
        // startAt: truoc do 15 phut, joinAfterMinutes la 10 phut -> qua muon de join
        RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                examId,
                1,
                OffsetDateTime.ofInstant(fixedInstant.minusSeconds(900), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC),
                10,
                10,
                "READY",
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                "REDIS"
        );

        when(activationResolver.getReadyActivation(examId)).thenReturn(metadata);
        when(assignmentResolver.resolveAssignmentId(examId, studentId)).thenReturn(assignmentId);

        assertThatThrownBy(() -> joinService.joinExam(examId, studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("EXAM_LATE_JOIN_CLOSED");
    }

    @Test
    void testJoinAfterLateWindowReturnsExistingInProgressSession() {
        // Neu hoc sinh da bat dau lam bai thi join lai phai tra session de frontend resume
        RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                examId,
                1,
                OffsetDateTime.ofInstant(fixedInstant.minusSeconds(900), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC),
                10,
                10,
                "READY",
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                "REDIS"
        );

        ExamSession session = new ExamSession();
        session.setId(UUID.randomUUID());
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);

        when(activationResolver.getReadyActivation(examId)).thenReturn(metadata);
        when(assignmentResolver.resolveAssignmentId(examId, studentId)).thenReturn(assignmentId);
        when(examSessionRepo.findByExamIdAndStudentIdAndAttemptNo(examId, studentId, 1))
                .thenReturn(Optional.of(session));
        when(examSessionRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = joinService.joinExam(examId, studentId);

        assertThat(response.getSessionId()).isEqualTo(session.getId());
        assertThat(response.getStatus()).isEqualTo(ExamSessionStatus.IN_PROGRESS);
        assertThat(response.isCanStart()).isTrue();
        assertThat(session.getLastSeenAt()).isEqualTo(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC));
    }
}
