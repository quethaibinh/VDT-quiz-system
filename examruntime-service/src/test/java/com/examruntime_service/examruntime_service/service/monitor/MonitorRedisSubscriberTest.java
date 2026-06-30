package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.StudentAlertDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonitorRedisSubscriberTest {

    @Test
    void forwardsMonitorMessageToTeacherTopicAndStudentAlertQueue() throws Exception {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        MonitorRedisSubscriber subscriber = new MonitorRedisSubscriber(messagingTemplate, objectMapper);
        UUID examId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentAlertDTO alert = StudentAlertDTO.builder()
                .examId(examId)
                .sessionId(UUID.randomUUID())
                .type("LOCKED")
                .message("MAX_VIOLATION_REACHED")
                .occurredAt(OffsetDateTime.now())
                .build();
        MonitorRealtimeMessageDTO message = MonitorRealtimeMessageDTO.builder()
                .messageType("STUDENT_ALERT")
                .examId(examId)
                .studentId(studentId)
                .alert(alert)
                .occurredAt(OffsetDateTime.now())
                .build();

        subscriber.handleMessage(objectMapper.writeValueAsString(message));

        ArgumentCaptor<MonitorRealtimeMessageDTO> messageCaptor =
                ArgumentCaptor.forClass(MonitorRealtimeMessageDTO.class);
        ArgumentCaptor<StudentAlertDTO> alertCaptor = ArgumentCaptor.forClass(StudentAlertDTO.class);
        verify(messagingTemplate).convertAndSend(eq(MonitorDestinations.teacherTopic(examId)), messageCaptor.capture());
        assertThat(messageCaptor.getValue().messageType()).isEqualTo("STUDENT_ALERT");
        assertThat(messageCaptor.getValue().examId()).isEqualTo(examId);
        assertThat(messageCaptor.getValue().studentId()).isEqualTo(studentId);
        verify(messagingTemplate).convertAndSendToUser(
                eq(studentId.toString()),
                eq(MonitorDestinations.studentAlertQueue(examId)),
                alertCaptor.capture()
        );
        assertThat(alertCaptor.getValue().type()).isEqualTo("LOCKED");
    }
}
