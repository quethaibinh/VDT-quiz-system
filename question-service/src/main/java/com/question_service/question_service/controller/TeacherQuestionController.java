package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.common.PageResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionDetailResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionUpsertRequestDTO;
import com.question_service.question_service.service.questions.TeacherQuestionCrudService;
import com.question_service.question_service.service.questions.TeacherQuestionSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/question-service/teacher/subjects/{subjectId}/questions")
/**
 * Cung cap API tim kiem ngan hang cau hoi ma giao vien duoc phep xem.
 */
public class TeacherQuestionController {

    private final TeacherQuestionSearchService searchService;
    private final TeacherQuestionCrudService crudService;

    public TeacherQuestionController(
            TeacherQuestionSearchService searchService,
            TeacherQuestionCrudService crudService
    ) {
        this.searchService = searchService;
        this.crudService = crudService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionDetailResponseDTO create(
            @PathVariable UUID subjectId,
            @Valid @RequestBody QuestionUpsertRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return crudService.create(subjectId, teacherId(principal), request);
    }

    @GetMapping
    public PageResponseDTO<QuestionResponseDTO> search(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String ownerScope,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) UUID collectionId,
            @RequestParam(required = false) String membership,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return searchService.search(
                subjectId,
                UUID.fromString(principal.userId()),
                keyword,
                topicId,
                difficulty,
                visibility,
                ownerScope,
                questionType,
                collectionId,
                membership,
                page,
                size,
                sort
        );
    }

    @GetMapping("/{questionId}")
    public QuestionDetailResponseDTO getDetail(
            @PathVariable UUID subjectId,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return crudService.getDetail(subjectId, questionId, teacherId(principal));
    }

    @PutMapping("/{questionId}")
    public QuestionDetailResponseDTO update(
            @PathVariable UUID subjectId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionUpsertRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return crudService.update(subjectId, questionId, teacherId(principal), request);
    }

    @PatchMapping("/{questionId}/archive")
    public QuestionDetailResponseDTO archive(
            @PathVariable UUID subjectId,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return crudService.archive(subjectId, questionId, teacherId(principal));
    }

    @PatchMapping("/{questionId}/restore")
    public QuestionDetailResponseDTO restore(
            @PathVariable UUID subjectId,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return crudService.restore(subjectId, questionId, teacherId(principal));
    }

    private UUID teacherId(QuestionUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
