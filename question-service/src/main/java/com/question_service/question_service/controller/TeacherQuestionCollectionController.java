package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.collections.*;
import com.question_service.question_service.model.dto.common.PageResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionResponseDTO;
import com.question_service.question_service.service.collections.QuestionCollectionItemService;
import com.question_service.question_service.service.collections.QuestionCollectionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/question-service/teacher/subjects/{subjectId}/question-collections")
/**
 * Cung cap API quan ly bo cau hoi va thanh vien cua bo cau hoi.
 */
public class TeacherQuestionCollectionController {

    private final QuestionCollectionService collectionService;
    private final QuestionCollectionItemService itemService;

    public TeacherQuestionCollectionController(
            QuestionCollectionService collectionService,
            QuestionCollectionItemService itemService
    ) {
        this.collectionService = collectionService;
        this.itemService = itemService;
    }

    @PostMapping
    public CollectionResponseDTO create(
            @PathVariable UUID subjectId,
            @Valid @RequestBody CollectionRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return collectionService.create(subjectId, teacherId(principal), request);
    }

    @GetMapping
    public PageResponseDTO<CollectionResponseDTO> list(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ownership,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return collectionService.list(
                subjectId, teacherId(principal), visibility, status, ownership,
                keyword, page, size, sort
        );
    }

    @GetMapping("/{collectionId}")
    public CollectionResponseDTO get(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return collectionService.get(subjectId, collectionId, teacherId(principal));
    }

    @PutMapping("/{collectionId}")
    public CollectionResponseDTO update(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @Valid @RequestBody CollectionRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return collectionService.update(
                subjectId, collectionId, teacherId(principal), request
        );
    }

    @DeleteMapping("/{collectionId}")
    public CollectionResponseDTO archive(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return collectionService.archive(subjectId, collectionId, teacherId(principal));
    }

    @PatchMapping("/{collectionId}/restore")
    public CollectionResponseDTO restore(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return collectionService.restore(subjectId, collectionId, teacherId(principal));
    }

    @GetMapping("/{collectionId}/questions")
    public PageResponseDTO<QuestionResponseDTO> listQuestions(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String ownerScope,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return itemService.listQuestions(
                subjectId, collectionId, teacherId(principal), keyword, topicId,
                difficulty, visibility, ownerScope, questionType, page, size, sort
        );
    }

    @PostMapping("/{collectionId}/questions")
    public BulkCollectionResultDTO addQuestions(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @Valid @RequestBody BulkQuestionIdsRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return itemService.add(
                subjectId, collectionId, teacherId(principal), request.questionIds()
        );
    }

    @PostMapping("/{collectionId}/questions/remove")
    public BulkCollectionResultDTO removeQuestions(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @Valid @RequestBody BulkQuestionIdsRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return itemService.remove(
                subjectId, collectionId, teacherId(principal), request.questionIds()
        );
    }

    @PostMapping("/{collectionId}/questions/add-by-filter")
    public BulkCollectionResultDTO addByFilter(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @RequestBody AddByFilterRequestDTO request,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        return itemService.addByFilter(
                subjectId, collectionId, teacherId(principal), request
        );
    }

    private UUID teacherId(QuestionUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
