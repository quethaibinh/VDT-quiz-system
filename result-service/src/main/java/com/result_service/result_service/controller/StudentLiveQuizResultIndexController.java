package com.result_service.result_service.controller;

import com.result_service.result_service.config.security.ResultUserPrincipal;
import com.result_service.result_service.model.dto.livequiz.StudentLiveQuizResultSummaryDTO;
import com.result_service.result_service.service.livequiz.LiveQuizResultIndexService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api/result-service/student/live-quiz-results")
public class StudentLiveQuizResultIndexController {

    private final LiveQuizResultIndexService indexService;

    public StudentLiveQuizResultIndexController(LiveQuizResultIndexService indexService) {
        this.indexService = indexService;
    }

    @GetMapping
    public List<StudentLiveQuizResultSummaryDTO> list(
            Authentication authentication,
            @RequestParam(required = false) UUID subjectId
    ) {
        return indexService.studentResults(studentId(authentication), subjectId);
    }

    private UUID studentId(Authentication authentication) {
        ResultUserPrincipal principal = (ResultUserPrincipal) authentication.getPrincipal();
        return UUID.fromString(principal.userId());
    }
}
