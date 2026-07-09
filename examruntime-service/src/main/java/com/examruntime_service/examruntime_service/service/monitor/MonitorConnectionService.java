package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.config.websocket.WebSocketPrincipal;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.ProctoringEvent;
import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringSeverity;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * Theo doi vong doi ket noi WebSocket cua student.
 *
 * STOMP disconnect khong di qua controller, nen can service nay de:
 * - Nho stompSessionId -> exam session khi CONNECT.
 * - Mark ONLINE khi connect hop le.
 * - Mark OFFLINE khi disconnect.
 * - Publish connection event cho teacher monitor.
 */
public class MonitorConnectionService {

    private static final String SESSION_ID_HEADER = "session-id";
    private static final String EXAM_ID_HEADER = "exam-id";

    private final ExamSessionRepo examSessionRepo;
    private final MonitorStateService monitorStateService;
    private final MonitorStateMapper mapper;
    private final MonitorEventPublisher publisher;
    private final Clock clock;
    private final Map<String, UUID> sessionsByStompSession = new ConcurrentHashMap<>();

    public MonitorConnectionService(
            ExamSessionRepo examSessionRepo,
            MonitorStateService monitorStateService,
            MonitorStateMapper mapper,
            MonitorEventPublisher publisher,
            Clock clock
    ) {
        this.examSessionRepo = examSessionRepo;
        this.monitorStateService = monitorStateService;
        this.mapper = mapper;
        this.publisher = publisher;
        this.clock = clock;
    }

    public void rememberConnection(StompHeaderAccessor accessor, WebSocketPrincipal principal) {
        if (!principal.hasRole("STUDENT")) {
            return;
        }
        // Frontend student nen gui hai native header nay luc CONNECT.
        // Neu thieu, connection van co the dung cho subscribe/send sau nay, nhung khong auto mark online/offline.
        String sessionIdValue = accessor.getFirstNativeHeader(SESSION_ID_HEADER);
        String examIdValue = accessor.getFirstNativeHeader(EXAM_ID_HEADER);
        if (sessionIdValue == null || examIdValue == null || accessor.getSessionId() == null) {
            return;
        }
        UUID sessionId = UUID.fromString(sessionIdValue);
        UUID examId = UUID.fromString(examIdValue);
        UUID studentId = UUID.fromString(principal.getName());
        ExamSession session = examSessionRepo.findById(sessionId).orElse(null);
        if (session == null || !session.getExamId().equals(examId) || !session.getStudentId().equals(studentId)) {
            // Khong throw o day de tranh pha handshake; authorization SEND se chan nghiem hon.
            return;
        }
        sessionsByStompSession.put(accessor.getSessionId(), sessionId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        SessionMonitorState state = monitorStateService.markOnline(session, now);
        publishConnectionEvent(session, state, ProctoringEventType.ONLINE, now);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        // Disconnect event chi cho stomp session id, nen phai tra nguoc ve runtime session da nho luc CONNECT.
        String stompSessionId = SimpMessageHeaderAccessor.wrap(event.getMessage()).getSessionId();
        UUID sessionId = stompSessionId != null ? sessionsByStompSession.remove(stompSessionId) : null;
        if (sessionId == null) {
            return;
        }
        ExamSession session = examSessionRepo.findById(sessionId).orElse(null);
        if (session == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        SessionMonitorState state = monitorStateService.markOffline(session, now);
        publishConnectionEvent(session, state, ProctoringEventType.OFFLINE, now);
    }

    private void publishConnectionEvent(
            ExamSession session,
            SessionMonitorState state,
            ProctoringEventType type,
            OffsetDateTime now
    ) {
        // ONLINE/OFFLINE khong ghi vao proctoring_events de tranh spam audit log.
        // Ta tao DTO tam thoi chi de gui realtime cho teacher.
        ProctoringEvent event = new ProctoringEvent();
        event.setExamId(session.getExamId());
        event.setSessionId(session.getId());
        event.setStudentId(session.getStudentId());
        event.setEventType(type);
        event.setSeverity(ProctoringSeverity.INFO);
        event.setOccurredAt(now);
        event.setReceivedAt(now);
        event.setMetadata("{}");
        MonitorRealtimeMessageDTO message = MonitorRealtimeMessageDTO.builder()
                .messageType("CONNECTION")
                .examId(session.getExamId())
                .sessionId(session.getId())
                .studentId(session.getStudentId())
                .participant(mapper.participant(session, state))
                .event(mapper.event(event))
                .occurredAt(now)
                .build();
        try {
            publisher.publish(session.getExamId(), message);
        } catch (Exception ignored) {
            // Connection event mat realtime thi REST snapshot van doc duoc onlineStatus moi tu Redis/DB.
        }
    }
}
