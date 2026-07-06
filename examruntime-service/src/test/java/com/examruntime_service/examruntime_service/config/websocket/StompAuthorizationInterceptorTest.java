package com.examruntime_service.examruntime_service.config.websocket;

import com.examruntime_service.examruntime_service.config.security.ExamRuntimeUserPrincipal;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import com.examruntime_service.examruntime_service.service.monitor.MonitorConnectionService;
import com.examruntime_service.examruntime_service.service.monitor.TeacherMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StompAuthorizationInterceptorTest {

    private final TeacherMonitorService teacherMonitorService = mock(TeacherMonitorService.class);
    private final MonitorConnectionService connectionService = mock(MonitorConnectionService.class);
    private final ExamSessionRepo examSessionRepo = mock(ExamSessionRepo.class);
    private final LiveQuizRoomRepo liveQuizRoomRepo = mock(LiveQuizRoomRepo.class);
    private final LiveQuizParticipantRepo liveQuizParticipantRepo = mock(LiveQuizParticipantRepo.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final StompAuthorizationInterceptor interceptor = new StompAuthorizationInterceptor(
            teacherMonitorService,
            connectionService,
            examSessionRepo,
            liveQuizRoomRepo,
            liveQuizParticipantRepo
    );

    @Test
    void preservesScheduledExamTeacherMonitorAuthorization() {
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(teacherMonitorService.canMonitor(examId, teacherId)).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                interceptor.preSend(message(StompCommand.SUBSCRIBE, "/topic/exams/%s/monitor".formatted(examId), teacher(teacherId)), channel)
        );

        verify(teacherMonitorService).canMonitor(examId, teacherId);
    }

    @Test
    void preservesScheduledExamStudentSendAuthorization() {
        UUID examId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        ExamSession session = new ExamSession();
        session.setExamId(examId);
        session.setStudentId(studentId);
        when(examSessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatNoException().isThrownBy(() ->
                interceptor.preSend(message(
                        StompCommand.SEND,
                        "/app/exams/%s/sessions/%s/events".formatted(examId, sessionId),
                        student(studentId)
                ), channel)
        );
    }

    @Test
    void authorizesLiveQuizTeacherOnlyForOwnedRoom() {
        UUID roomId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        LiveQuizRoom room = new LiveQuizRoom();
        room.setOwnerTeacherId(teacherId);
        when(liveQuizRoomRepo.findById(roomId)).thenReturn(Optional.of(room));

        assertThatNoException().isThrownBy(() ->
                interceptor.preSend(message(StompCommand.SUBSCRIBE, "/topic/live-quizzes/%s/lobby".formatted(roomId), teacher(teacherId)), channel)
        );

        assertThatThrownBy(() ->
                interceptor.preSend(message(StompCommand.SUBSCRIBE, "/topic/live-quizzes/%s/lobby".formatted(roomId), teacher(UUID.randomUUID())), channel)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SUBSCRIBE_FORBIDDEN");
    }

    @Test
    void authorizesLiveQuizStudentQueueOnlyForJoinedParticipant() {
        UUID roomId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(liveQuizParticipantRepo.existsByRoomIdAndStudentId(roomId, studentId)).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                interceptor.preSend(message(StompCommand.SUBSCRIBE, "/user/queue/live-quizzes/%s".formatted(roomId), student(studentId)), channel)
        );

        assertThatThrownBy(() ->
                interceptor.preSend(message(StompCommand.SUBSCRIBE, "/user/queue/live-quizzes/%s".formatted(roomId), student(UUID.randomUUID())), channel)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SUBSCRIBE_FORBIDDEN");
    }

    private Message<byte[]> message(StompCommand command, String destination, WebSocketPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private WebSocketPrincipal teacher(UUID teacherId) {
        return new WebSocketPrincipal(new ExamRuntimeUserPrincipal(teacherId.toString(), "teacher", "ROLE_TEACHER"));
    }

    private WebSocketPrincipal student(UUID studentId) {
        return new WebSocketPrincipal(new ExamRuntimeUserPrincipal(studentId.toString(), "student", "ROLE_STUDENT"));
    }
}
