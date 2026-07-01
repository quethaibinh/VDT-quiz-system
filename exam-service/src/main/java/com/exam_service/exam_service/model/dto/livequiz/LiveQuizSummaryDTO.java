package com.exam_service.exam_service.model.dto.livequiz;

import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.LiveQuizJoinPolicy;

import java.util.UUID;

public record LiveQuizSummaryDTO(
        UUID id,
        String code,
        String title,
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
        long version
) {
}
