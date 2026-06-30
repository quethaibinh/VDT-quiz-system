package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.dto.cache.InternalExamAssignmentDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorEventDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorParticipantDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.ProctoringEvent;
import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class MonitorStateMapper {

    public MonitorParticipantDTO participant(ExamSession session, SessionMonitorState state) {
        return participant(session, state, null);
    }

    public MonitorParticipantDTO participant(
            ExamSession session,
            SessionMonitorState state,
            InternalExamAssignmentDTO.AssignmentDetail assignment
    ) {
        String status = resolveStatus(session, state);
        return MonitorParticipantDTO.builder()
                .sessionId(session.getId())
                .studentId(session.getStudentId())
                .studentCode(resolveStudentCode(session.getStudentId(), assignment))
                .studentName(resolveStudentName(session.getStudentId(), assignment))
                .status(status)
                .answeredCount(state != null ? state.getAnsweredCount() : session.getAnsweredCount())
                .totalQuestions(state != null ? state.getTotalQuestions() : 0)
                .totalViolationCount(state != null ? state.getTotalViolationCount() : session.getViolationCount())
                .riskScore(state != null ? state.getRiskScore() : BigDecimal.ZERO)
                .riskLevel(state != null ? state.getRiskLevel() : RiskLevel.LOW)
                .locked(state != null ? state.isLocked() : session.getStatus() == ExamSessionStatus.LOCKED)
                .lastSeenAt(session.getLastSeenAt())
                .lastHeartbeatAt(state != null ? state.getLastHeartbeatAt() : null)
                .lastEventAt(state != null ? state.getLastEventAt() : null)
                .build();
    }

    public MonitorParticipantDTO notJoinedParticipant(InternalExamAssignmentDTO.AssignmentDetail assignment) {
        return MonitorParticipantDTO.builder()
                .sessionId(null)
                .studentId(assignment.getStudentId())
                .studentCode(resolveStudentCode(assignment.getStudentId(), assignment))
                .studentName(resolveStudentName(assignment.getStudentId(), assignment))
                .status("NOT_JOIN")
                .answeredCount(0)
                .totalQuestions(0)
                .totalViolationCount(0)
                .riskScore(BigDecimal.ZERO)
                .riskLevel(RiskLevel.LOW)
                .locked(false)
                .lastSeenAt(null)
                .lastHeartbeatAt(null)
                .lastEventAt(null)
                .build();
    }

    public MonitorEventDTO event(ProctoringEvent event) {
        return MonitorEventDTO.builder()
                .id(event.getId())
                .examId(event.getExamId())
                .sessionId(event.getSessionId())
                .studentId(event.getStudentId())
                .eventType(event.getEventType())
                .severity(event.getSeverity())
                .occurredAt(event.getOccurredAt())
                .receivedAt(event.getReceivedAt())
                .metadata(event.getMetadata())
                .countInSession(event.getCountInSession())
                .build();
    }

    private String resolveStatus(ExamSession session, SessionMonitorState state) {
        if (session.getStatus() == ExamSessionStatus.SUBMITTED
                || session.getStatus() == ExamSessionStatus.AUTO_SUBMITTED
                || session.getStatus() == ExamSessionStatus.EXPIRED
                || session.getStatus() == ExamSessionStatus.LOCKED) {
            return session.getStatus().name();
        }
        if (state != null && state.getOnlineStatus() != null) {
            return state.getOnlineStatus().name();
        }
        return session.getStatus().name();
    }

    private String resolveStudentCode(UUID studentId, InternalExamAssignmentDTO.AssignmentDetail assignment) {
        return assignment != null && assignment.getStudentCode() != null && !assignment.getStudentCode().isBlank()
                ? assignment.getStudentCode()
                : studentId.toString();
    }

    private String resolveStudentName(UUID studentId, InternalExamAssignmentDTO.AssignmentDetail assignment) {
        return assignment != null && assignment.getStudentName() != null && !assignment.getStudentName().isBlank()
                ? assignment.getStudentName()
                : studentId.toString();
    }
}
