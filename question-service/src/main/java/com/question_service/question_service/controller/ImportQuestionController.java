package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.imports.ImportQuestionResultDTO;
import com.question_service.question_service.service.imports.ImportQuestionService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/question-service")
public class ImportQuestionController {

    private final ImportQuestionService importQuestionService;

    public ImportQuestionController(ImportQuestionService importQuestionService) {
        this.importQuestionService = importQuestionService;
    }

    @PostMapping(
            value = "/teacher/subjects/{subjectId}/questions/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ImportQuestionResultDTO importQuestions(
            @PathVariable UUID subjectId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal QuestionUserPrincipal principal
    ) throws Exception {
        return importQuestionService.importQuestions(
                subjectId,
                UUID.fromString(principal.userId()),
                file
        );
    }

}
