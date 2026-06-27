package com.examruntime_service.examruntime_service.controller;

import com.examruntime_service.examruntime_service.config.security.ExamRuntimeUserPrincipal;
import com.examruntime_service.examruntime_service.model.dto.session.StudentJoinResponseDTO;
import com.examruntime_service.examruntime_service.service.session.StudentExamJoinService;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveResponseDTO;
import com.examruntime_service.examruntime_service.model.dto.session.StudentPaperResponseDTO;
import com.examruntime_service.examruntime_service.service.session.autoSave.StudentAutosaveService;
import com.examruntime_service.examruntime_service.service.session.StudentExamStartService;
import com.examruntime_service.examruntime_service.service.session.resume.StudentSessionResumeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/examruntime-service/student")
// Controller cho cac api runtime thi cua hoc sinh
public class StudentExamSessionController {

    private final StudentExamJoinService joinService;
    private final StudentExamStartService startService;
    private final StudentSessionResumeService resumeService;
    private final StudentAutosaveService autosaveService;

    public StudentExamSessionController(
            StudentExamJoinService joinService,
            StudentExamStartService startService,
            StudentSessionResumeService resumeService,
            StudentAutosaveService autosaveService
    ) {
        this.joinService = joinService;
        this.startService = startService;
        this.resumeService = resumeService;
        this.autosaveService = autosaveService;
    }

    @PostMapping("/exams/{examId}/join")
    // Api cho phep hoc sinh dang ky vao ca thi de chuan bi lam bai
    public StudentJoinResponseDTO joinExam(
            @PathVariable UUID examId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        UUID studentId = UUID.fromString(principal.userId());
        return joinService.joinExam(examId, studentId);
    }

    @PostMapping("/exams/{examId}/start")
    // Api cho hoc sinh thuc su bat dau lam de thi, sinh de thi ca nhan
    public StudentPaperResponseDTO startExam(
            @PathVariable UUID examId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        UUID studentId = UUID.fromString(principal.userId());
        return startService.startExam(examId, studentId);
    }

    @GetMapping("/sessions/{sessionId}")
    // Api phuc hoi lai de thi va cac cau tra loi da luu truoc do
    public StudentPaperResponseDTO resumeSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        UUID studentId = UUID.fromString(principal.userId());
        return resumeService.resumeSession(sessionId, studentId);
    }

    @PutMapping("/sessions/{sessionId}/answers")
    // Api tu dong luu nhap dap an cua hoc sinh
    public AutosaveResponseDTO autosave(
            @PathVariable UUID sessionId,
            @RequestBody AutosaveRequestDTO request,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        UUID studentId = UUID.fromString(principal.userId());
        return autosaveService.autosave(sessionId, request, studentId);
    }
}
