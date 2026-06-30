package com.examruntime_service.examruntime_service.config.websocket;

import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.service.monitor.MonitorConnectionService;
import com.examruntime_service.examruntime_service.service.monitor.TeacherMonitorService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
/**
 * Guard cho cac STOMP frame sau handshake.
 *
 * Handshake chi biet "ai dang ket noi"; interceptor nay kiem tra "duoc lam gi":
 * - Teacher chi subscribe topic cua exam minh so huu.
 * - Student chi gui event cho session cua chinh minh.
 * - Student chi subscribe user queue alert rieng.
 */
public class StompAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern TEACHER_TOPIC =
            Pattern.compile("^/topic/exams/([0-9a-fA-F\\-]{36})/monitor$");
    private static final Pattern STUDENT_SEND =
            Pattern.compile("^/app/exams/([0-9a-fA-F\\-]{36})/sessions/([0-9a-fA-F\\-]{36})/events$");

    private final TeacherMonitorService teacherMonitorService;
    private final MonitorConnectionService connectionService;
    private final ExamSessionRepo examSessionRepo;

    public StompAuthorizationInterceptor(
            TeacherMonitorService teacherMonitorService,
            MonitorConnectionService connectionService,
            ExamSessionRepo examSessionRepo
    ) {
        this.teacherMonitorService = teacherMonitorService;
        this.connectionService = connectionService;
        this.examSessionRepo = examSessionRepo;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        WebSocketPrincipal principal = principal(accessor.getUser());
        if (principal == null) {
            throw new IllegalArgumentException("WS_UNAUTHENTICATED");
        }
        if (command == StompCommand.CONNECT) {
            // Neu student gui native header exam-id/session-id luc CONNECT,
            // ta co the danh dau online va nho stompSessionId de offline khi disconnect.
            connectionService.rememberConnection(accessor, principal);
        }
        if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(principal, accessor.getDestination());
        }
        if (command == StompCommand.SEND) {
            authorizeSend(principal, accessor.getDestination());
        }
        return message;
    }

    private void authorizeSubscribe(WebSocketPrincipal principal, String destination) {
        if (destination == null) {
            throw new IllegalArgumentException("DESTINATION_REQUIRED");
        }
        Matcher teacherTopic = TEACHER_TOPIC.matcher(destination);
        if (teacherTopic.matches()) {
            UUID examId = UUID.fromString(teacherTopic.group(1));
            // Quyen monitor dua tren ownerTeacherId cua activation metadata.
            // Khong cho teacher bat ky subscribe topic exam khac.
            if (!principal.hasRole("TEACHER")
                    || !teacherMonitorService.canMonitor(examId, UUID.fromString(principal.getName()))) {
                throw new IllegalArgumentException("SUBSCRIBE_FORBIDDEN");
            }
            return;
        }
        if (destination.startsWith("/user/queue/exams/")) {
            if (!principal.hasRole("STUDENT")) {
                throw new IllegalArgumentException("SUBSCRIBE_FORBIDDEN");
            }
            return;
        }
        throw new IllegalArgumentException("SUBSCRIBE_FORBIDDEN");
    }

    private void authorizeSend(WebSocketPrincipal principal, String destination) {
        Matcher matcher = destination != null ? STUDENT_SEND.matcher(destination) : null;
        if (matcher == null || !matcher.matches() || !principal.hasRole("STUDENT")) {
            throw new IllegalArgumentException("SEND_FORBIDDEN");
        }
        UUID examId = UUID.fromString(matcher.group(1));
        UUID sessionId = UUID.fromString(matcher.group(2));
        UUID studentId = UUID.fromString(principal.getName());
        // Check DB session ownership de student khong spoof sessionId cua nguoi khac.
        boolean ownsSession = examSessionRepo.findById(sessionId)
                .map(session -> session.getExamId().equals(examId) && session.getStudentId().equals(studentId))
                .orElse(false);
        if (!ownsSession) {
            throw new IllegalArgumentException("SEND_FORBIDDEN");
        }
    }

    private WebSocketPrincipal principal(Principal principal) {
        return principal instanceof WebSocketPrincipal value ? value : null;
    }
}
