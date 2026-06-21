package com.question_service.question_service.model.dto.collections;

import com.question_service.question_service.model.entity.enums.ContentFormat;
import com.question_service.question_service.model.entity.enums.Difficulty;

import java.util.List;
import java.util.UUID;

/**
 * Mot cau hoi day du tai thoi diem giao vien chot lich thi.
 */
public record ExamQuestionSnapshotDTO(
        UUID questionId,
        long questionVersion,
        Difficulty difficulty,
        String type,
        String content,
        ContentFormat contentFormat,
        Double defaultScore,
        List<ExamOptionSnapshotDTO> options
) {
}
