package com.examruntime_service.examruntime_service.controller;

import com.examruntime_service.examruntime_service.config.security.ExamRuntimeUserPrincipal;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorSnapshotDTO;
import com.examruntime_service.examruntime_service.service.monitor.TeacherMonitorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/examruntime-service/teacher/exams")
// REST snapshot cho teacher monitor. Frontend co the polling endpoint nay de phuc hoi state
// neu WebSocket mat ket noi hoac miss Redis Pub/Sub message.
public class TeacherMonitorController {

    private final TeacherMonitorService monitorService;

    public TeacherMonitorController(TeacherMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/{examId}/monitor")
    public MonitorSnapshotDTO snapshot(
            @PathVariable UUID examId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        UUID teacherId = UUID.fromString(principal.userId());
        // TeacherMonitorService se check teacher co phai owner cua exam khong truoc khi tra state.
        return monitorService.snapshot(examId, teacherId);
    }
}
