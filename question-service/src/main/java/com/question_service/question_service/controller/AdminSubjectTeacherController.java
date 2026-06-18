package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.subjects.AssignTeacherRequestDTO;
import com.question_service.question_service.model.dto.subjects.SubjectTeacherResponseDTO;
import com.question_service.question_service.service.subjects.SubjectTeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/api/admin/question-service/subjects/{subjectId}/teachers")
/**
 * Cung cap API gan va go giao vien khoi mon hoc.
 */
public class AdminSubjectTeacherController {

    private final SubjectTeacherService subjectTeacherService;

    public AdminSubjectTeacherController(SubjectTeacherService subjectTeacherService) {
        this.subjectTeacherService = subjectTeacherService;
    }

    @PostMapping
    public SubjectTeacherResponseDTO assignTeacher(
            @PathVariable UUID subjectId,
            @RequestBody AssignTeacherRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return subjectTeacherService.assignTeacher(
                subjectId,
                request.getTeacherId(),
                UUID.fromString(principal.userId())
        );
    }

    @GetMapping
    public List<SubjectTeacherResponseDTO> listTeachers(@PathVariable UUID subjectId) {
        return subjectTeacherService.listTeachers(subjectId);
    }

    @DeleteMapping("/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTeacher(
            @PathVariable UUID subjectId,
            @PathVariable UUID teacherId
    ) {
        subjectTeacherService.removeTeacher(subjectId, teacherId);
    }

}
