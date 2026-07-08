package com.question_service.question_service.controller;

import com.question_service.question_service.model.dto.questions.QuestionMediaSignRequestDTO;
import com.question_service.question_service.model.dto.questions.QuestionMediaSignResponseDTO;
import com.question_service.question_service.service.media.QuestionMediaStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/internal/question-service/question-media")
public class InternalQuestionMediaController {

    private final QuestionMediaStorageService mediaStorageService;

    public InternalQuestionMediaController(QuestionMediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping("/urls")
    public QuestionMediaSignResponseDTO signedUrls(@RequestBody QuestionMediaSignRequestDTO request) {
        List<String> objectKeys = request != null && request.objectKeys() != null
                ? request.objectKeys()
                : List.of();
        Map<String, String> urls = new LinkedHashMap<>();
        objectKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .forEach(key -> {
                    String safeKey = mediaStorageService.validateObjectKey(key);
                    urls.put(safeKey, mediaStorageService.signedReadUrl(safeKey));
                });
        return new QuestionMediaSignResponseDTO(urls, mediaStorageService.signedUrlTtlSeconds());
    }
}
