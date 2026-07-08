package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.questions.QuestionMediaUploadResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionMediaUrlResponseDTO;
import com.question_service.question_service.service.media.QuestionMediaStorageService;
import com.question_service.question_service.service.subjects.TeacherSubjectAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/question-service/teacher/subjects/{subjectId}/question-media")
public class TeacherQuestionMediaController {

    private final TeacherSubjectAccessService subjectAccessService;
    private final QuestionMediaStorageService mediaStorageService;

    public TeacherQuestionMediaController(
            TeacherSubjectAccessService subjectAccessService,
            QuestionMediaStorageService mediaStorageService
    ) {
        this.subjectAccessService = subjectAccessService;
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionMediaUploadResponseDTO upload(
            @PathVariable UUID subjectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId(principal));
        return new QuestionMediaUploadResponseDTO(mediaStorageService.upload(file));
    }

    @GetMapping("/url")
    public QuestionMediaUrlResponseDTO signedUrl(
            @PathVariable UUID subjectId,
            @RequestParam String objectKey,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId(principal));
        String safeKey = mediaStorageService.validateObjectKey(objectKey);
        return new QuestionMediaUrlResponseDTO(
                safeKey,
                mediaStorageService.signedReadUrl(safeKey),
                mediaStorageService.signedUrlTtlSeconds()
        );
    }

    private UUID teacherId(QuestionUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
