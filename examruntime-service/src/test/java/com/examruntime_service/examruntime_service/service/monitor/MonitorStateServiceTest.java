package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.client.ExamServiceAssignmentClient;
import com.examruntime_service.examruntime_service.model.dto.cache.InternalExamAssignmentDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorSnapshotDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.MonitorOnlineStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.ProctoringEventRepo;
import com.examruntime_service.examruntime_service.repository.SessionMonitorStateRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitorStateServiceTest {

    private StringRedisTemplate redisTemplate;
    private ExamSessionRepo examSessionRepo;
    private SessionMonitorStateRepo monitorStateRepo;
    private ProctoringEventRepo proctoringEventRepo;
    private ExamServiceAssignmentClient assignmentClient;
    private MonitorEventPublisher publisher;
    private MonitorStateService service;

    private final UUID examId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        examSessionRepo = mock(ExamSessionRepo.class);
        monitorStateRepo = mock(SessionMonitorStateRepo.class);
        proctoringEventRepo = mock(ProctoringEventRepo.class);
        assignmentClient = mock(ExamServiceAssignmentClient.class);
        publisher = mock(MonitorEventPublisher.class);
        service = new MonitorStateService(
                redisTemplate,
                examSessionRepo,
                monitorStateRepo,
                proctoringEventRepo,
                assignmentClient,
                new MonitorStateMapper(),
                publisher,
                new ObjectMapper(),
                Clock.fixed(fixedInstant, ZoneOffset.UTC)
        );
    }

    @Test
    void updateAnsweredCountPublishesProgressMessage() {
        ExamSession session = session();
        SessionMonitorState state = state();
        OffsetDateTime autosaveAt = OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC);
        when(monitorStateRepo.findById(sessionId)).thenReturn(Optional.of(state));

        service.updateAnsweredCount(session, 2, autosaveAt);

        ArgumentCaptor<MonitorRealtimeMessageDTO> captor = ArgumentCaptor.forClass(MonitorRealtimeMessageDTO.class);
        verify(monitorStateRepo).save(state);
        verify(publisher).publish(eq(examId), captor.capture());
        MonitorRealtimeMessageDTO message = captor.getValue();
        assertThat(message.messageType()).isEqualTo("PROGRESS");
        assertThat(message.event()).isNull();
        assertThat(message.participant()).isNotNull();
        assertThat(message.participant().answeredCount()).isEqualTo(2);
        assertThat(message.participant().sessionId()).isEqualTo(sessionId);
    }

    @Test
    void readSnapshotMergesAssignmentsWithRuntimeSessions() {
        UUID notJoinedStudentId = UUID.randomUUID();
        UUID notJoinedAssignmentId = UUID.randomUUID();
        ExamSession joinedSession = session();
        SessionMonitorState joinedState = state();
        InternalExamAssignmentDTO.AssignmentDetail joinedAssignment =
                new InternalExamAssignmentDTO.AssignmentDetail(UUID.randomUUID(), studentId, "S001", "Student One");
        InternalExamAssignmentDTO.AssignmentDetail notJoinedAssignment =
                new InternalExamAssignmentDTO.AssignmentDetail(notJoinedAssignmentId, notJoinedStudentId, "S002", "Student Two");

        when(assignmentClient.getAssignments(examId))
                .thenReturn(new InternalExamAssignmentDTO(examId, List.of(joinedAssignment, notJoinedAssignment)));
        when(examSessionRepo.findAllByExamId(examId)).thenReturn(List.of(joinedSession));
        when(monitorStateRepo.findAllByExamId(examId)).thenReturn(List.of(joinedState));
        when(proctoringEventRepo.findAllByExamIdOrderByOccurredAtDesc(eq(examId), any())).thenReturn(List.of());

        MonitorSnapshotDTO snapshot = service.readSnapshot(examId);

        assertThat(snapshot.participants()).hasSize(2);
        assertThat(snapshot.participants())
                .anySatisfy(participant -> {
                    assertThat(participant.sessionId()).isEqualTo(sessionId);
                    assertThat(participant.studentId()).isEqualTo(studentId);
                    assertThat(participant.studentCode()).isEqualTo("S001");
                    assertThat(participant.studentName()).isEqualTo("Student One");
                    assertThat(participant.status()).isEqualTo("ONLINE");
                })
                .anySatisfy(participant -> {
                    assertThat(participant.sessionId()).isNull();
                    assertThat(participant.studentId()).isEqualTo(notJoinedStudentId);
                    assertThat(participant.studentCode()).isEqualTo("S002");
                    assertThat(participant.studentName()).isEqualTo("Student Two");
                    assertThat(participant.status()).isEqualTo("NOT_JOIN");
                    assertThat(participant.answeredCount()).isZero();
                    assertThat(participant.totalViolationCount()).isZero();
                });
    }

    @Test
    void readSnapshotKeepsSessionWhenAssignmentSnapshotMissesStudent() {
        ExamSession joinedSession = session();
        SessionMonitorState joinedState = state();
        when(assignmentClient.getAssignments(examId)).thenReturn(new InternalExamAssignmentDTO(examId, List.of()));
        when(examSessionRepo.findAllByExamId(examId)).thenReturn(List.of(joinedSession));
        when(monitorStateRepo.findAllByExamId(examId)).thenReturn(List.of(joinedState));
        when(proctoringEventRepo.findAllByExamIdOrderByOccurredAtDesc(eq(examId), any())).thenReturn(List.of());

        MonitorSnapshotDTO snapshot = service.readSnapshot(examId);

        assertThat(snapshot.participants()).hasSize(1);
        assertThat(snapshot.participants().getFirst().studentId()).isEqualTo(studentId);
        assertThat(snapshot.participants().getFirst().sessionId()).isEqualTo(sessionId);
    }

    private ExamSession session() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setQuestionOrder("[\"" + UUID.randomUUID() + "\",\"" + UUID.randomUUID() + "\"]");
        return session;
    }

    private SessionMonitorState state() {
        SessionMonitorState state = new SessionMonitorState();
        state.setSessionId(sessionId);
        state.setExamId(examId);
        state.setStudentId(studentId);
        state.setOnlineStatus(MonitorOnlineStatus.ONLINE);
        state.setRiskScore(BigDecimal.ZERO);
        state.setRiskLevel(RiskLevel.LOW);
        state.setAnsweredCount(1);
        state.setTotalQuestions(2);
        return state;
    }
}
