package com.result_service.result_service.controller;

import com.result_service.result_service.config.security.ResultUserPrincipal;
import com.result_service.result_service.model.dto.livequiz.StudentLiveQuizFinalResultDTO;
import com.result_service.result_service.service.livequiz.LiveQuizResultQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/result-service/student/live-quizzes/{roomId}/result")
public class StudentLiveQuizResultController {

    private final LiveQuizResultQueryService queryService;

    public StudentLiveQuizResultController(LiveQuizResultQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public StudentLiveQuizFinalResultDTO result(Authentication authentication, @PathVariable UUID roomId) {
        // Student chi xem summary ket qua cua chinh minh, khong xem answer key/detail tung cau.
        return queryService.studentResult(studentId(authentication), roomId);
    }

    private UUID studentId(Authentication authentication) {
        // userId trong principal la studentId da duoc Gateway gan vao request.
        ResultUserPrincipal principal = (ResultUserPrincipal) authentication.getPrincipal();
        return UUID.fromString(principal.userId());
    }
}
