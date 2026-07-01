package com.exam_service.exam_service.model.dto.livequiz;

import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.LiveQuizJoinPolicy;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LiveQuizDetailDTO(
        UUID id,
        String code,
        String title,
        String description,
        UUID subjectId,
        String subjectName,
        UUID collectionId,
        String collectionName,
        int questionCount,
        boolean shuffleQuestions,
        boolean showLeaderboard,
        boolean showCorrectAnswer,
        LiveQuizJoinPolicy joinPolicy,
        ExamStatus status,
        int snapshotVersion,
        OffsetDateTime preparedAt,
        long version
) {
}
