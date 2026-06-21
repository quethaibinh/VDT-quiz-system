package com.question_service.question_service.model.dto.collections;

import java.util.List;
import java.util.UUID;

/**
 * Candidate pool bat bien ma Exam Service se luu khi chuyen sang SCHEDULED.
 */
public record ExamCollectionSnapshotDTO(
        UUID collectionId,
        UUID subjectId,
        String subjectName,
        String name,
        List<ExamQuestionSnapshotDTO> questions
) {
}
