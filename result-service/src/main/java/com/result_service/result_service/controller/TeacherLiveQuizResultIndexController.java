package com.result_service.result_service.controller;

import com.result_service.result_service.config.security.ResultUserPrincipal;
import com.result_service.result_service.model.dto.common.PageResponseDTO;
import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultIndexDTO;
import com.result_service.result_service.service.livequiz.LiveQuizResultIndexService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/result-service/teacher/subjects/{subjectId}/live-quiz-results")
public class TeacherLiveQuizResultIndexController {

    private final LiveQuizResultIndexService indexService;

    public TeacherLiveQuizResultIndexController(LiveQuizResultIndexService indexService) {
        this.indexService = indexService;
    }

    @GetMapping
    public PageResponseDTO<TeacherLiveQuizResultIndexDTO> list(
            Authentication authentication,
            @PathVariable UUID subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return indexService.teacherSubjectResults(teacherId(authentication), subjectId, page, size);
    }

    private UUID teacherId(Authentication authentication) {
        ResultUserPrincipal principal = (ResultUserPrincipal) authentication.getPrincipal();
        return UUID.fromString(principal.userId());
    }
}
