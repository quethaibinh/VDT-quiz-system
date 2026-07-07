package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.dto.monitor.ProctoringEventRequestDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.ProctoringEvent;
import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.MonitorOnlineStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.ProctoringEventRepo;
import com.examruntime_service.examruntime_service.repository.SessionMonitorStateRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import com.examruntime_service.examruntime_service.service.session.RuntimeActivationResolver;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class ProctoringEventServiceTest {

    private ExamSessionRepo examSessionRepo;
    private SessionMonitorStateRepo monitorStateRepo;
    private ProctoringEventRepo proctoringEventRepo;
    private MonitorStateService monitorStateService;
    private MonitorEventPublisher publisher;
    private RuntimeActivationResolver activationResolver;
    private ProctoringEventService service;

    private final UUID examId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        examSessionRepo = mock(ExamSessionRepo.class);
        monitorStateRepo = mock(SessionMonitorStateRepo.class);
        proctoringEventRepo = mock(ProctoringEventRepo.class);
        monitorStateService = mock(MonitorStateService.class);
        publisher = mock(MonitorEventPublisher.class);
        MonitorStateMapper mapper = new MonitorStateMapper();
        MonitorRiskPolicy riskPolicy = new MonitorRiskPolicy(2);
        activationResolver = mock(RuntimeActivationResolver.class);

        service = new ProctoringEventService(
                examSessionRepo,
                monitorStateRepo,
                proctoringEventRepo,
                riskPolicy,
                monitorStateService,
                mapper,
                publisher,
                new SessionLockService(),
                new ObjectMapper(),
                Clock.fixed(fixedInstant, ZoneId.of("UTC")),
                activationResolver,
                4096
        );
    }

    @Test
    void recordsViolationAndLocksAtThreshold() {
        ExamSession session = session();
        SessionMonitorState state = state();
        state.setTotalViolationCount(1);
        state.setRiskScore(BigDecimal.ONE);
        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(monitorStateRepo.findBySessionIdForUpdate(sessionId)).thenReturn(Optional.of(state));
        when(proctoringEventRepo.findBySessionIdAndClientEventId(sessionId, "event-1"))
                .thenReturn(Optional.empty());
        when(proctoringEventRepo.save(any())).thenAnswer(invocation -> {
            ProctoringEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                examId,
                1,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                0,
                0,
                null,
                2,
                "LOCK",
                RuntimeActivationMetadata.STATUS_READY,
                OffsetDateTime.now(),
                RuntimeActivationMetadata.SOURCE_REDIS
        );
        when(activationResolver.getReadyActivation(examId)).thenReturn(metadata);

        var message = service.recordEvent(examId, sessionId, studentId, request("event-1"));

        assertThat(session.getStatus()).isEqualTo(ExamSessionStatus.LOCKED);
        assertThat(state.isLocked()).isTrue();
        assertThat(state.getTotalViolationCount()).isEqualTo(2);
        assertThat(state.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(message.alert()).isNotNull();
        verify(proctoringEventRepo).save(any());
        verify(publisher).publish(eq(examId), any());
    }

    @Test
    void duplicateClientEventDoesNotInsertOrIncrement() {
        ExamSession session = session();
        SessionMonitorState state = state();
        ProctoringEvent existing = new ProctoringEvent();
        existing.setId(UUID.randomUUID());
        existing.setExamId(examId);
        existing.setSessionId(sessionId);
        existing.setStudentId(studentId);
        existing.setEventType(ProctoringEventType.TAB_HIDDEN);
        existing.setOccurredAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC));
        existing.setReceivedAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC));
        existing.setMetadata("{}");

        when(examSessionRepo.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(monitorStateRepo.findById(sessionId)).thenReturn(Optional.of(state));
        when(proctoringEventRepo.findBySessionIdAndClientEventId(sessionId, "event-1"))
                .thenReturn(Optional.of(existing));

        service.recordEvent(examId, sessionId, studentId, request("event-1"));

        assertThat(state.getTotalViolationCount()).isZero();
        verify(proctoringEventRepo, never()).save(any());
        verify(publisher, never()).publish(any(), any());
    }

    private ProctoringEventRequestDTO request(String clientEventId) {
        ProctoringEventRequestDTO request = new ProctoringEventRequestDTO();
        request.setClientEventId(clientEventId);
        request.setEventType(ProctoringEventType.TAB_HIDDEN);
        request.setOccurredAt(OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC));
        request.setMetadata("{\"fullscreen\":false}");
        return request;
    }

    private ExamSession session() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
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
        return state;
    }
}
