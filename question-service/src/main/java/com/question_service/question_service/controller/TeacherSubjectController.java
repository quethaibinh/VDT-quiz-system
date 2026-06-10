package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.subjects.SubjectResponseDTO;
import com.question_service.question_service.service.subjects.SubjectTeacherService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api/question-service/teacher/subjects")
public class TeacherSubjectController {

    private final SubjectTeacherService subjectTeacherService;

    public TeacherSubjectController(SubjectTeacherService subjectTeacherService) {
        this.subjectTeacherService = subjectTeacherService;
    }

    @GetMapping
    public List<SubjectResponseDTO> listSubjects(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return subjectTeacherService.listSubjectsForTeacher(teacherId(principal), status, keyword);
    }

    @GetMapping("/{subjectId}")
    public SubjectResponseDTO getSubject(
            @PathVariable UUID subjectId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return subjectTeacherService.getSubjectForTeacher(teacherId(principal), subjectId);
    }

    private UUID teacherId(QuestionUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }

}
