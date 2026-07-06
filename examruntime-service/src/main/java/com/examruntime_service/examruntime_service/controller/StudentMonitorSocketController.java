package com.examruntime_service.examruntime_service.controller;

import com.examruntime_service.examruntime_service.config.websocket.WebSocketPrincipal;
import com.examruntime_service.examruntime_service.model.dto.monitor.ProctoringEventRequestDTO;
import com.examruntime_service.examruntime_service.service.monitor.ProctoringEventService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class StudentMonitorSocketController {

    private final ProctoringEventService proctoringEventService;

    public StudentMonitorSocketController(ProctoringEventService proctoringEventService) {
        this.proctoringEventService = proctoringEventService;
    }

    @MessageMapping("/exams/{examId}/sessions/{sessionId}/events")
    public void recordEvent(
            @DestinationVariable UUID examId,
            @DestinationVariable UUID sessionId,
            @Valid ProctoringEventRequestDTO request,
            Principal principal
    ) {
        WebSocketPrincipal wsPrincipal = (WebSocketPrincipal) principal;
        UUID studentId = UUID.fromString(wsPrincipal.getName());
        proctoringEventService.recordEvent(examId, sessionId, studentId, request);
    }
}
