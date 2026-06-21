package com.question_service.question_service.controller;

import com.question_service.question_service.model.dto.collections.ExamCollectionMetadataDTO;
import com.question_service.question_service.model.dto.collections.ExamCollectionSnapshotDTO;
import com.question_service.question_service.service.collections.QuestionCollectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/internal/question-service/subjects/{subjectId}/collections")
public class InternalExamCollectionController {

    private final QuestionCollectionService collectionService;

    public InternalExamCollectionController(QuestionCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping("/{collectionId}/exam-metadata")
    public ExamCollectionMetadataDTO getExamMetadata(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @RequestParam UUID teacherId
    ) {
        return collectionService.getExamMetadata(subjectId, collectionId, teacherId);
    }

    @GetMapping("/{collectionId}/exam-snapshot")
    public ExamCollectionSnapshotDTO getExamSnapshot(
            @PathVariable UUID subjectId,
            @PathVariable UUID collectionId,
            @RequestParam UUID teacherId
    ) {
        return collectionService.getExamSnapshot(subjectId, collectionId, teacherId);
    }
}
