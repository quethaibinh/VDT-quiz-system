package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.model.dto.cache.ExamAnswerKeyDTO;
import com.exam_service.exam_service.model.dto.cache.ExamPaperPoolDTO;
import com.exam_service.exam_service.service.exams.ExamSnapshotReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/internal/exam-service/exams/{examId}")
/**
 * Fallback PostgreSQL khi consumer khong tim thay snapshot trong Redis.
 */
public class InternalExamSnapshotController {

    private final ExamSnapshotReadService snapshotReadService;

    public InternalExamSnapshotController(ExamSnapshotReadService snapshotReadService) {
        this.snapshotReadService = snapshotReadService;
    }

    @GetMapping("/paper-pool")
    public ExamPaperPoolDTO getPaperPool(@PathVariable UUID examId) {
        return snapshotReadService.getPaperPool(examId);
    }

    @GetMapping("/answer-key")
    public ExamAnswerKeyDTO getAnswerKey(@PathVariable UUID examId) {
        return snapshotReadService.getAnswerKey(examId);
    }
}
