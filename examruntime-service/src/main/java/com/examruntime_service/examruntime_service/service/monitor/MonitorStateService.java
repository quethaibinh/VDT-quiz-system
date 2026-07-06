package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.client.ExamServiceAssignmentClient;
import com.examruntime_service.examruntime_service.model.dto.cache.InternalExamAssignmentDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorParticipantDTO;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * Trung tam quan ly trang thai nong cua man hinh giam sat.
 *
 * Luong chinh:
 * - Runtime service goi class nay khi student join/start/autosave/submit/connect.
 * - PostgreSQL giu snapshot ben vung de khoi phuc va audit.
 * - Redis hash `session:{sessionId}:state` giu cac field thay doi nhanh de teacher doc nhanh.
 * - REST snapshot se merge Redis truoc, DB sau, nen neu Pub/Sub miss event thi UI van sua duoc khi refresh.
 */
public class MonitorStateService {

    private static final int DEFAULT_EVENT_LIMIT = 50;

    private final StringRedisTemplate redisTemplate;
    private final ExamSessionRepo examSessionRepo;
    private final SessionMonitorStateRepo monitorStateRepo;
    private final ProctoringEventRepo proctoringEventRepo;
    private final ExamServiceAssignmentClient assignmentClient;
    private final MonitorStateMapper mapper;
    private final MonitorEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String instanceId = UUID.randomUUID().toString();

    public MonitorStateService(
            StringRedisTemplate redisTemplate,
            ExamSessionRepo examSessionRepo,
            SessionMonitorStateRepo monitorStateRepo,
            ProctoringEventRepo proctoringEventRepo,
            ExamServiceAssignmentClient assignmentClient,
            MonitorStateMapper mapper,
            MonitorEventPublisher publisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.examSessionRepo = examSessionRepo;
        this.monitorStateRepo = monitorStateRepo;
        this.proctoringEventRepo = proctoringEventRepo;
        this.assignmentClient = assignmentClient;
        this.mapper = mapper;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    // Tao hoac cap nhat ban ghi monitor khi session bat dau xuat hien trong runtime.
    // Ham nay duoc goi tu join/start de dam bao teacher monitor co du lieu nen truoc khi co WebSocket event.
    public void ensureSessionRegistered(ExamSession session) {
        SessionMonitorState state = monitorStateRepo.findById(session.getId())
                .orElseGet(() -> createState(session));
        state.setAnsweredCount(session.getAnsweredCount());
        state.setTotalQuestions(resolveTotalQuestions(session));
        state.setLocked(session.getStatus() == ExamSessionStatus.LOCKED);
        state.setLockedReason(session.getLockedReason());
        state.setTotalViolationCount(session.getViolationCount());
        monitorStateRepo.save(state);
        writeHotState(session, state);
    }

    @Transactional
    // Autosave da ghi dap an vao Redis/DB xong thi cap nhat answeredCount cho monitor.
    // Chi ghi cac field monitor can thiet, khong ghi de nhung field autosave khac trong cung Redis hash.
    public void updateAnsweredCount(ExamSession session, int answeredCount, OffsetDateTime lastAutosaveAt) {
        SessionMonitorState state = monitorStateRepo.findById(session.getId())
                .orElseGet(() -> createState(session));
        state.setAnsweredCount(answeredCount);
        state.setTotalQuestions(resolveTotalQuestions(session));
        state.setLastEventAt(lastAutosaveAt);
        monitorStateRepo.save(state);
        putFields(session, Map.of(
                "answeredCount", Integer.toString(answeredCount),
                "lastAutosaveAt", lastAutosaveAt.toString()
        ));
        publishProgress(session, state, lastAutosaveAt);
    }

    private void publishProgress(ExamSession session, SessionMonitorState state, OffsetDateTime occurredAt) {
        try {
            // Autosave chi tao thay doi tien do, khong tao audit event moi.
            // Vi vay message realtime chi gui participant moi nhat de dashboard cap nhat answeredCount.
            MonitorRealtimeMessageDTO message = MonitorRealtimeMessageDTO.builder()
                    .messageType("PROGRESS")
                    .examId(session.getExamId())
                    .sessionId(session.getId())
                    .studentId(session.getStudentId())
                    .participant(mapper.participant(session, state))
                    .event(null)
                    .alert(null)
                    .instanceId(instanceId)
                    .occurredAt(occurredAt)
                    .build();
            publisher.publish(session.getExamId(), message);
        } catch (Exception ignored) {
            // Pub/Sub la kenh realtime best-effort. REST snapshot van doc duoc answeredCount moi tu DB/Redis.
        }
    }

    @Transactional
    // Submit la trang thai ket thuc luong lam bai, nen monitor khong nen tiep tuc hien student la ONLINE.
    // DB snapshot va Redis hot state deu duoc cap nhat de REST snapshot va realtime cung nhat quan.
    public void markSubmitted(ExamSession session) {
        SessionMonitorState state = monitorStateRepo.findById(session.getId())
                .orElseGet(() -> createState(session));
        state.setOnlineStatus(MonitorOnlineStatus.OFFLINE);
        state.setAnsweredCount(session.getAnsweredCount());
        state.setTotalViolationCount(session.getViolationCount());
        state.setLocked(session.getStatus() == ExamSessionStatus.LOCKED);
        state.setLockedReason(session.getLockedReason());
        monitorStateRepo.save(state);
        writeHotState(session, state);
    }

    @Transactional
    // Khi WebSocket CONNECT hop le, danh dau session online va publish sau o MonitorConnectionService.
    public SessionMonitorState markOnline(ExamSession session, OffsetDateTime now) {
        SessionMonitorState state = monitorStateRepo.findById(session.getId())
                .orElseGet(() -> createState(session));
        state.setOnlineStatus(MonitorOnlineStatus.ONLINE);
        state.setLastHeartbeatAt(now);
        state.setLastEventAt(now);
        monitorStateRepo.save(state);
        writeHotState(session, state);
        return state;
    }

    @Transactional
    // Khi WebSocket DISCONNECT, danh dau offline. Heartbeat chi nen cap nhat Redis/DB co kiem soat de tranh spam DB.
    public SessionMonitorState markOffline(ExamSession session, OffsetDateTime now) {
        SessionMonitorState state = monitorStateRepo.findById(session.getId())
                .orElseGet(() -> createState(session));
        state.setOnlineStatus(MonitorOnlineStatus.OFFLINE);
        state.setLastHeartbeatAt(now);
        state.setLastEventAt(now);
        monitorStateRepo.save(state);
        writeHotState(session, state);
        return state;
    }

    @Transactional(readOnly = true)
    // Teacher dashboard doc snapshot tai day.
    // Thu tu uu tien: danh sach session tu DB -> state DB -> merge field moi nhat tu Redis -> recent events tu DB.
    public MonitorSnapshotDTO readSnapshot(UUID examId) {
        InternalExamAssignmentDTO assignmentSnapshot = assignmentClient.getAssignments(examId);
        List<InternalExamAssignmentDTO.AssignmentDetail> assignments =
                assignmentSnapshot != null && assignmentSnapshot.getAssignments() != null
                        ? assignmentSnapshot.getAssignments()
                        : List.of();
        List<ExamSession> sessions = examSessionRepo.findAllByExamId(examId);
        Map<UUID, SessionMonitorState> dbStates = monitorStateRepo.findAllByExamId(examId).stream()
                .collect(Collectors.toMap(SessionMonitorState::getSessionId, Function.identity()));
        List<SessionMonitorState> mergedStates = sessions.stream()
                .map(session -> mergeHotState(session, dbStates.get(session.getId())))
                .toList();
        Map<UUID, SessionMonitorState> bySession = mergedStates.stream()
                .collect(Collectors.toMap(SessionMonitorState::getSessionId, Function.identity()));
        Map<UUID, ExamSession> sessionByStudent = new LinkedHashMap<>();
        sessions.forEach(session -> sessionByStudent.putIfAbsent(session.getStudentId(), session));
        Map<UUID, InternalExamAssignmentDTO.AssignmentDetail> assignmentByStudent = new LinkedHashMap<>();
        assignments.stream()
                .filter(assignment -> assignment.getStudentId() != null)
                .forEach(assignment -> assignmentByStudent.putIfAbsent(assignment.getStudentId(), assignment));

        List<MonitorParticipantDTO> participants = new ArrayList<>();
        for (InternalExamAssignmentDTO.AssignmentDetail assignment : assignmentByStudent.values()) {
            ExamSession session = sessionByStudent.get(assignment.getStudentId());
            if (session == null) {
                participants.add(mapper.notJoinedParticipant(assignment));
            } else {
                participants.add(mapper.participant(session, bySession.get(session.getId()), assignment));
            }
        }
        sessions.stream()
                .filter(session -> !assignmentByStudent.containsKey(session.getStudentId()))
                .map(session -> mapper.participant(session, bySession.get(session.getId())))
                .forEach(participants::add);

        return MonitorSnapshotDTO.builder()
                .examId(examId)
                .serverTime(OffsetDateTime.now(clock))
                .participants(participants)
                .events(proctoringEventRepo.findAllByExamIdOrderByOccurredAtDesc(
                                examId,
                                PageRequest.of(0, DEFAULT_EVENT_LIMIT)
                        ).stream()
                        .map(mapper::event)
                        .toList())
                .build();
    }

    public static String sessionStateKey(UUID sessionId) {
        return "session:%s:state".formatted(sessionId);
    }

    public static String examSessionsKey(UUID examId) {
        return "exam:monitor:sessions:%s".formatted(examId);
    }

    public static String examEventsChannel(UUID examId) {
        return "exam:monitor:events:%s".formatted(examId);
    }

    private SessionMonitorState createState(ExamSession session) {
        // Gia tri mac dinh phai an toan: UNKNOWN/LOW/khong locked.
        // Cac field nay se duoc update dan khi co autosave, violation, connect/disconnect.
        SessionMonitorState state = new SessionMonitorState();
        state.setSessionId(session.getId());
        state.setExamId(session.getExamId());
        state.setStudentId(session.getStudentId());
        state.setOnlineStatus(MonitorOnlineStatus.UNKNOWN);
        state.setAnsweredCount(session.getAnsweredCount());
        state.setTotalQuestions(resolveTotalQuestions(session));
        state.setTotalViolationCount(session.getViolationCount());
        state.setRiskScore(BigDecimal.ZERO);
        state.setRiskLevel(RiskLevel.LOW);
        state.setLocked(session.getStatus() == ExamSessionStatus.LOCKED);
        state.setLockedReason(session.getLockedReason());
        return state;
    }

    public void writeHotState(ExamSession session, SessionMonitorState state) {
        // Redis hash nay dung chung voi autosave (`session:{sessionId}:state`).
        // Vi vay chi put nhung field monitor can dung, khong delete/replace toan bo hash.
        Map<String, String> fields = new HashMap<>();
        fields.put("examId", session.getExamId().toString());
        fields.put("studentId", session.getStudentId().toString());
        fields.put("onlineStatus", state.getOnlineStatus().name());
        fields.put("answeredCount", Integer.toString(state.getAnsweredCount()));
        fields.put("totalQuestions", Integer.toString(state.getTotalQuestions()));
        fields.put("totalViolationCount", Integer.toString(state.getTotalViolationCount()));
        fields.put("riskScore", state.getRiskScore().toPlainString());
        fields.put("riskLevel", state.getRiskLevel().name());
        fields.put("locked", Boolean.toString(state.isLocked()));
        if (state.getLastHeartbeatAt() != null) {
            fields.put("lastHeartbeatAt", state.getLastHeartbeatAt().toString());
        }
        if (state.getLastEventAt() != null) {
            fields.put("lastEventAt", state.getLastEventAt().toString());
        }
        if (state.getLockedReason() != null) {
            fields.put("lockedReason", state.getLockedReason());
        }
        putFields(session, fields);
    }

    private void putFields(ExamSession session, Map<String, String> fields) {
        try {
            // Set TTL theo deadline + 24h de state song du cho resume/result/monitor sau ca thi.
            redisTemplate.opsForHash().putAll(sessionStateKey(session.getId()), fields);
            redisTemplate.opsForSet().add(examSessionsKey(session.getExamId()), session.getId().toString());
            Duration ttl = resolveTtl(session, OffsetDateTime.now(clock));
            redisTemplate.expire(sessionStateKey(session.getId()), ttl);
            redisTemplate.expire(examSessionsKey(session.getExamId()), ttl);
        } catch (Exception ignored) {
            // Monitor Redis is a hot projection. PostgreSQL snapshot remains source of recovery.
        }
    }

    private SessionMonitorState mergeHotState(ExamSession session, SessionMonitorState fallback) {
        SessionMonitorState state = fallback != null ? fallback : createState(session);
        try {
            // Redis la projection nong, co the mat/corrupt rieng le.
            // Neu parse fail thi bo qua field loi va tiep tuc dung DB fallback.
            Map<Object, Object> hot = redisTemplate.opsForHash().entries(sessionStateKey(session.getId()));
            if (hot == null || hot.isEmpty()) {
                return state;
            }
            state.setAnsweredCount(intValue(hot, "answeredCount", state.getAnsweredCount()));
            state.setTotalQuestions(intValue(hot, "totalQuestions", state.getTotalQuestions()));
            state.setTotalViolationCount(intValue(hot, "totalViolationCount", state.getTotalViolationCount()));
            state.setRiskScore(decimalValue(hot, "riskScore", state.getRiskScore()));
            state.setRiskLevel(enumValue(hot, "riskLevel", RiskLevel.class, state.getRiskLevel()));
            state.setOnlineStatus(enumValue(hot, "onlineStatus", MonitorOnlineStatus.class, state.getOnlineStatus()));
            state.setLocked(booleanValue(hot, "locked", state.isLocked()));
            state.setLastHeartbeatAt(timeValue(hot, "lastHeartbeatAt", state.getLastHeartbeatAt()));
            state.setLastEventAt(timeValue(hot, "lastEventAt", state.getLastEventAt()));
            Object lockedReason = hot.get("lockedReason");
            if (lockedReason instanceof String value && !value.isBlank()) {
                state.setLockedReason(value);
            }
        } catch (Exception ignored) {
        }
        return state;
    }

    private int resolveTotalQuestions(ExamSession session) {
        try {
            List<UUID> ids = objectMapper.readValue(session.getQuestionOrder(), new TypeReference<List<UUID>>() {});
            return ids.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Duration resolveTtl(ExamSession session, OffsetDateTime now) {
        if (session.getServerDeadlineAt() == null) {
            return Duration.ofHours(24);
        }
        Duration ttl = Duration.between(now, session.getServerDeadlineAt().plusHours(24));
        return ttl.isNegative() || ttl.isZero() ? Duration.ofHours(24) : ttl;
    }

    private int intValue(Map<Object, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private BigDecimal decimalValue(Map<Object, Object> map, String key, BigDecimal fallback) {
        Object value = map.get(key);
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean booleanValue(Map<Object, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof String text && !text.isBlank() ? Boolean.parseBoolean(text) : fallback;
    }

    private OffsetDateTime timeValue(Map<Object, Object> map, String key, OffsetDateTime fallback) {
        Object value = map.get(key);
        if (value instanceof String text && !text.isBlank()) {
            try {
                return OffsetDateTime.parse(text);
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private <T extends Enum<T>> T enumValue(Map<Object, Object> map, String key, Class<T> enumClass, T fallback) {
        Object value = map.get(key);
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Enum.valueOf(enumClass, text);
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }
}
