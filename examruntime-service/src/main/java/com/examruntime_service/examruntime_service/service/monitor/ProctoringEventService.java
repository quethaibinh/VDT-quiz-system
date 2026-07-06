package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.ProctoringEventRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.StudentAlertDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.ProctoringEvent;
import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.MonitorOnlineStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.ProctoringEventRepo;
import com.examruntime_service.examruntime_service.repository.SessionMonitorStateRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import com.examruntime_service.examruntime_service.util.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
/**
 * Xu ly event giam sat do browser student gui len qua STOMP.
 *
 * Day la diem quyet dinh nghiep vu:
 * - Xac thuc session thuoc dung exam va dung student.
 * - Chong ghi trung bang clientEventId.
 * - Ghi audit vao proctoring_events.
 * - Cap nhat counter/risk/lock vao session_monitor_states va Redis hot state.
 * - Publish realtime message qua Redis Pub/Sub de instance dang giu teacher WebSocket nhan duoc.
 */
public class ProctoringEventService {

    private final ExamSessionRepo examSessionRepo;
    private final SessionMonitorStateRepo monitorStateRepo;
    private final ProctoringEventRepo proctoringEventRepo;
    private final MonitorRiskPolicy riskPolicy;
    private final MonitorStateService monitorStateService;
    private final MonitorStateMapper mapper;
    private final MonitorEventPublisher publisher;
    private final SessionLockService lockService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int metadataMaxBytes;
    private final String instanceId = UUID.randomUUID().toString();

    public ProctoringEventService(
            ExamSessionRepo examSessionRepo,
            SessionMonitorStateRepo monitorStateRepo,
            ProctoringEventRepo proctoringEventRepo,
            MonitorRiskPolicy riskPolicy,
            MonitorStateService monitorStateService,
            MonitorStateMapper mapper,
            MonitorEventPublisher publisher,
            SessionLockService lockService,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${examruntime.monitor.metadata-max-bytes:4096}") int metadataMaxBytes
    ) {
        this.examSessionRepo = examSessionRepo;
        this.monitorStateRepo = monitorStateRepo;
        this.proctoringEventRepo = proctoringEventRepo;
        this.riskPolicy = riskPolicy;
        this.monitorStateService = monitorStateService;
        this.mapper = mapper;
        this.publisher = publisher;
        this.lockService = lockService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.metadataMaxBytes = metadataMaxBytes;
    }

    @Transactional
    // Transaction nay bao gom DB audit + update counter + auto-lock.
    // Pub/Sub duoc publish sau khi state da duoc cap nhat, va neu publish fail thi REST snapshot van la duong sua.
    public MonitorRealtimeMessageDTO recordEvent(
            UUID examId,
            UUID sessionId,
            UUID studentId,
            ProctoringEventRequestDTO request
    ) {
        validateRequest(request);
        // Lock session de tranh hai event song song cung tang counter/lock sai.
        ExamSession session = examSessionRepo.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));
        if (!session.getExamId().equals(examId)) {
            throw new ConflictException("SESSION_EXAM_MISMATCH");
        }
        if (!session.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("UNAUTHORIZED_SESSION");
        }
        if (isTerminal(session.getStatus())) {
            throw new ConflictException("SESSION_NOT_IN_PROGRESS");
        }

        ProctoringEvent existing = proctoringEventRepo
                .findBySessionIdAndClientEventId(sessionId, request.getClientEventId())
                .orElse(null);
        if (existing != null) {
            // Retry WebSocket/REST fallback co the gui lai cung clientEventId.
            // Truong hop nay tra ve state hien tai nhung khong insert event va khong tang violation.
            return buildMessage(session, ensureState(session), existing, null);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        SessionMonitorState state = monitorStateRepo.findBySessionIdForUpdate(sessionId)
                .orElseGet(() -> {
                    monitorStateService.ensureSessionRegistered(session);
                    return monitorStateRepo.findBySessionIdForUpdate(sessionId)
                            .orElseThrow(() -> new NotFoundException("MONITOR_STATE_NOT_FOUND"));
                });

        ProctoringEventType type = request.getEventType();
        boolean violation = isViolation(type);
        if (type == ProctoringEventType.ONLINE) {
            // ONLINE/OFFLINE la tin hieu ket noi, khong tinh la vi pham.
            state.setOnlineStatus(MonitorOnlineStatus.ONLINE);
            state.setLastHeartbeatAt(now);
        }
        if (type == ProctoringEventType.OFFLINE) {
            state.setOnlineStatus(MonitorOnlineStatus.OFFLINE);
            state.setOfflineCount(state.getOfflineCount() + 1);
            state.setLastHeartbeatAt(now);
        }
        if (violation) {
            // Moi event vi pham co weight rieng. Risk score cho phep sau nay tinh nang tinh vi hon
            // ma khong phai doi schema counter co ban.
            incrementTypeCount(state, type);
            state.setTotalViolationCount(state.getTotalViolationCount() + 1);
            state.setRiskScore(state.getRiskScore().add(riskPolicy.weight(type)));
            state.setRiskLevel(riskPolicy.level(state.getRiskScore(), state.getTotalViolationCount()));
            session.setViolationCount(state.getTotalViolationCount());
        }
        state.setLastEventAt(now);

        ProctoringEvent event = new ProctoringEvent();
        // proctoring_events la append-only audit log; khong dua Redis Pub/Sub lam bang chung duy nhat.
        event.setExamId(examId);
        event.setSessionId(sessionId);
        event.setStudentId(studentId);
        event.setEventType(type);
        event.setSeverity(riskPolicy.severity(type));
        event.setOccurredAt(request.getOccurredAt());
        event.setReceivedAt(now);
        event.setClientEventId(request.getClientEventId());
        event.setDurationMs(request.getDurationMs());
        event.setCountInSession(state.getTotalViolationCount());
        event.setMetadata(normalizeMetadata(request.getMetadata()));
        ProctoringEvent savedEvent = proctoringEventRepo.save(event);

        StudentAlertDTO alert = null;
        if (riskPolicy.shouldLock(state.getRiskScore(), state.getTotalViolationCount())) {
            // Lock chi thuc hien mot lan. Cac event sau khi session LOCKED se bi reject o dau ham.
            boolean locked = lockService.lock(session, state, "MAX_VIOLATION_REACHED");
            if (locked) {
                alert = StudentAlertDTO.builder()
                        .examId(examId)
                        .sessionId(sessionId)
                        .type("LOCKED")
                        .message("MAX_VIOLATION_REACHED")
                        .occurredAt(now)
                        .build();
            }
        }

        examSessionRepo.save(session);
        monitorStateRepo.save(state);
        monitorStateService.writeHotState(session, state);
        MonitorRealtimeMessageDTO message = buildMessage(session, state, savedEvent, alert);
        try {
            // Redis Pub/Sub chi la kenh day realtime giua cac instance.
            // Neu Redis loi, DB da co state dung va teacher co the refresh REST snapshot.
            publisher.publish(examId, message);
        } catch (Exception ignored) {
            // Durable audit/state already committed by the transaction; REST snapshot repairs missed realtime events.
        }
        return message;
    }

    private SessionMonitorState ensureState(ExamSession session) {
        return monitorStateRepo.findById(session.getId()).orElseGet(() -> {
            monitorStateService.ensureSessionRegistered(session);
            return monitorStateRepo.findById(session.getId()).orElseThrow();
        });
    }

    private MonitorRealtimeMessageDTO buildMessage(
            ExamSession session,
            SessionMonitorState state,
            ProctoringEvent event,
            StudentAlertDTO alert
    ) {
        return MonitorRealtimeMessageDTO.builder()
                .messageType(alert != null ? "STUDENT_ALERT" : "MONITOR_EVENT")
                .examId(session.getExamId())
                .sessionId(session.getId())
                .studentId(session.getStudentId())
                .participant(mapper.participant(session, state))
                .event(mapper.event(event))
                .alert(alert)
                .instanceId(instanceId)
                .occurredAt(OffsetDateTime.now(clock))
                .build();
    }

    private void validateRequest(ProctoringEventRequestDTO request) {
        // Validate som de chan payload lon/loi, tranh ghi JSON rac vao audit log.
        if (request == null || request.getClientEventId() == null || request.getClientEventId().isBlank()) {
            throw new IllegalArgumentException("CLIENT_EVENT_ID_REQUIRED");
        }
        if (request.getClientEventId().length() > 128) {
            throw new IllegalArgumentException("CLIENT_EVENT_ID_TOO_LONG");
        }
        if (request.getEventType() == null) {
            throw new IllegalArgumentException("EVENT_TYPE_REQUIRED");
        }
        if (request.getOccurredAt() == null) {
            throw new IllegalArgumentException("OCCURRED_AT_REQUIRED");
        }
        String metadata = request.getMetadata() != null ? request.getMetadata() : "{}";
        if (metadata.getBytes(StandardCharsets.UTF_8).length > metadataMaxBytes) {
            throw new IllegalArgumentException("METADATA_TOO_LARGE");
        }
    }

    private String normalizeMetadata(String metadata) {
        String payload = metadata == null || metadata.isBlank() ? "{}" : metadata;
        try {
            // Normalize thanh JSON string hop le de Postgres jsonb va frontend doc lai on dinh.
            return objectMapper.writeValueAsString(objectMapper.readTree(payload));
        } catch (Exception exception) {
            throw new IllegalArgumentException("INVALID_METADATA_JSON");
        }
    }

    private boolean isTerminal(ExamSessionStatus status) {
        return status == ExamSessionStatus.SUBMITTED
                || status == ExamSessionStatus.AUTO_SUBMITTED
                || status == ExamSessionStatus.EXPIRED
                || status == ExamSessionStatus.LOCKED;
    }

    private boolean isViolation(ProctoringEventType type) {
        // Weight = 0 nghia la event chi phuc vu trang thai/diagnostic, khong cong loi.
        return riskPolicy.weight(type).compareTo(BigDecimal.ZERO) > 0;
    }

    private void incrementTypeCount(SessionMonitorState state, ProctoringEventType type) {
        switch (type) {
            case TAB_HIDDEN -> state.setTabHiddenCount(state.getTabHiddenCount() + 1);
            case WINDOW_BLUR -> state.setWindowBlurCount(state.getWindowBlurCount() + 1);
            case FULLSCREEN_EXIT -> state.setFullscreenExitCount(state.getFullscreenExitCount() + 1);
            default -> {
            }
        }
    }
}
