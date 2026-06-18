package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.subjects.SubjectRequestDTO;
import com.question_service.question_service.model.dto.subjects.SubjectResponseDTO;
import com.question_service.question_service.service.subjects.SubjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api/admin/question-service/subjects")
/**
 * Cung cap API quan tri vong doi cua mon hoc.
 */
public class AdminSubjectController {

    private final SubjectService subjectService;

    public AdminSubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    public SubjectResponseDTO createSubject(
            @RequestBody SubjectRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return subjectService.createSubject(adminId(principal), request);
    }

    @GetMapping
    public List<SubjectResponseDTO> listSubjects(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return subjectService.listSubjects(status, keyword);
    }

    @GetMapping("/{subjectId}")
    public SubjectResponseDTO getSubject(@PathVariable UUID subjectId) {
        return subjectService.getSubject(subjectId);
    }

    @PutMapping("/{subjectId}")
    public SubjectResponseDTO updateSubject(
            @PathVariable UUID subjectId,
            @RequestBody SubjectRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return subjectService.updateSubject(adminId(principal), subjectId, request);
    }

    @PatchMapping("/{subjectId}/archive")
    public SubjectResponseDTO archiveSubject(
            @PathVariable UUID subjectId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return subjectService.archiveSubject(adminId(principal), subjectId);
    }

    @PatchMapping("/{subjectId}/restore")
    public SubjectResponseDTO restoreSubject(
            @PathVariable UUID subjectId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return subjectService.restoreSubject(adminId(principal), subjectId);
    }

    private UUID adminId(QuestionUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }

}
