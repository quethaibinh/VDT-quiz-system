package com.result_service.result_service.controller;

import com.result_service.result_service.config.security.ResultUserPrincipal;
import com.result_service.result_service.model.dto.results.StudentResultDetailDTO;
import com.result_service.result_service.model.dto.results.StudentResultSummaryDTO;
import com.result_service.result_service.service.results.ResultReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api/result-service/student")
public class StudentResultController {

    private final ResultReviewService resultReviewService;

    public StudentResultController(ResultReviewService resultReviewService) {
        this.resultReviewService = resultReviewService;
    }

    @GetMapping("/results")
    public List<StudentResultSummaryDTO> list(Authentication authentication) {
        return resultReviewService.studentResults(studentId(authentication));
    }

    @GetMapping("/exams/{examId}/result")
    public StudentResultDetailDTO detail(Authentication authentication, @PathVariable UUID examId) {
        return resultReviewService.studentResult(studentId(authentication), examId);
    }

    private UUID studentId(Authentication authentication) {
        ResultUserPrincipal principal = (ResultUserPrincipal) authentication.getPrincipal();
        return UUID.fromString(principal.userId());
    }
}
