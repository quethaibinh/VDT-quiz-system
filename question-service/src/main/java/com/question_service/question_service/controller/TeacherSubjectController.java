package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.subjects.SubjectResponseDTO;
import com.question_service.question_service.model.dto.topics.TopicResponseDTO;
import com.question_service.question_service.service.subjects.SubjectTeacherService;
import com.question_service.question_service.service.topics.TeacherTopicService;
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
/**
 * Cung cap danh sach mon hoc ma giao vien dang duoc phan cong.
 */
public class TeacherSubjectController {

    private final SubjectTeacherService subjectTeacherService;
    private final TeacherTopicService teacherTopicService;

    public TeacherSubjectController(
            SubjectTeacherService subjectTeacherService,
            TeacherTopicService teacherTopicService
    ) {
        this.subjectTeacherService = subjectTeacherService;
        this.teacherTopicService = teacherTopicService;
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

    @GetMapping("/{subjectId}/topics")
    public List<TopicResponseDTO> listTopics(
            @PathVariable UUID subjectId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return teacherTopicService.listTopics(subjectId, teacherId(principal));
    }

    private UUID teacherId(QuestionUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }

}
