package com.result_service.result_service.controller;

import com.result_service.result_service.config.security.ResultUserPrincipal;
import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultDetailDTO;
import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultsDTO;
import com.result_service.result_service.service.livequiz.LiveQuizResultExportService;
import com.result_service.result_service.service.livequiz.LiveQuizResultQueryService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/result-service/teacher/live-quizzes/{roomId}/results")
public class TeacherLiveQuizResultController {

    private final LiveQuizResultQueryService queryService;
    private final LiveQuizResultExportService exportService;

    public TeacherLiveQuizResultController(
            LiveQuizResultQueryService queryService,
            LiveQuizResultExportService exportService
    ) {
        this.queryService = queryService;
        this.exportService = exportService;
    }

    @GetMapping
    public TeacherLiveQuizResultsDTO list(
            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        // Teacher xem bang xep hang chot sau khi room CLOSED va consumer da ingest event.
        // Gioi han size de tranh mot request lay qua nhieu row tren lop UI.
        return queryService.teacherResults(teacherId(authentication), roomId, page, Math.min(size, 100));
    }

    @GetMapping("/{resultId}")
    public TeacherLiveQuizResultDetailDTO detail(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID resultId
    ) {
        // Detail tra ve answer-level data, chi danh cho owner teacher cua room.
        return queryService.teacherDetail(teacherId(authentication), roomId, resultId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(Authentication authentication, @PathVariable UUID roomId) {
        // Export dung cung rule phan quyen voi man hinh teacher result.
        byte[] bytes = exportService.exportRoom(teacherId(authentication), roomId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("live-quiz-results-%s.xlsx".formatted(roomId))
                        .build()
                        .toString())
                .body(bytes);
    }

    private UUID teacherId(Authentication authentication) {
        // Gateway da xac thuc user; controller chi lay userId tu principal noi bo.
        ResultUserPrincipal principal = (ResultUserPrincipal) authentication.getPrincipal();
        return UUID.fromString(principal.userId());
    }
}
