package com.result_service.result_service.controller;

import com.result_service.result_service.config.security.ResultUserPrincipal;
import com.result_service.result_service.model.dto.results.PublishResultsRequestDTO;
import com.result_service.result_service.model.dto.results.PublishResultsResponseDTO;
import com.result_service.result_service.model.dto.results.ScoreAdjustmentRequestDTO;
import com.result_service.result_service.model.dto.results.TeacherExamResultsDTO;
import com.result_service.result_service.model.dto.results.TeacherResultDetailDTO;
import com.result_service.result_service.model.dto.results.TeacherResultRowDTO;
import com.result_service.result_service.service.results.ResultReviewService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/result-service/teacher/exams/{examId}/results")
public class TeacherResultController {

    private final ResultReviewService resultReviewService;

    public TeacherResultController(ResultReviewService resultReviewService) {
        this.resultReviewService = resultReviewService;
    }

    @GetMapping
    public TeacherExamResultsDTO list(
            Authentication authentication,
            @PathVariable UUID examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return resultReviewService.teacherExamResults(teacherId(authentication), examId, page, Math.min(size, 100));
    }

    @GetMapping("/{resultId}")
    public TeacherResultDetailDTO detail(
            Authentication authentication,
            @PathVariable UUID examId,
            @PathVariable UUID resultId
    ) {
        return resultReviewService.teacherResultDetail(teacherId(authentication), examId, resultId);
    }

    @PatchMapping("/{resultId}/score")
    public TeacherResultRowDTO adjustScore(
            Authentication authentication,
            @PathVariable UUID examId,
            @PathVariable UUID resultId,
            @RequestBody ScoreAdjustmentRequestDTO request
    ) {
        return resultReviewService.adjustScore(
                teacherId(authentication),
                examId,
                resultId,
                request.adjustedScore(),
                request.reason()
        );
    }

    @PostMapping("/{resultId}/publish")
    public PublishResultsResponseDTO publishOne(
            Authentication authentication,
            @PathVariable UUID examId,
            @PathVariable UUID resultId
    ) {
        return resultReviewService.publish(teacherId(authentication), examId, java.util.List.of(resultId));
    }

    @PostMapping("/publish")
    public PublishResultsResponseDTO publishMany(
            Authentication authentication,
            @PathVariable UUID examId,
            @RequestBody(required = false) PublishResultsRequestDTO request
    ) {
        return resultReviewService.publish(
                teacherId(authentication),
                examId,
                request != null ? request.resultIds() : null
        );
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(Authentication authentication, @PathVariable UUID examId) {
        byte[] bytes = resultReviewService.exportExamResults(teacherId(authentication), examId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("exam-results-%s.xlsx".formatted(examId))
                        .build()
                        .toString())
                .body(bytes);
    }

    private UUID teacherId(Authentication authentication) {
        ResultUserPrincipal principal = (ResultUserPrincipal) authentication.getPrincipal();
        return UUID.fromString(principal.userId());
    }
}
