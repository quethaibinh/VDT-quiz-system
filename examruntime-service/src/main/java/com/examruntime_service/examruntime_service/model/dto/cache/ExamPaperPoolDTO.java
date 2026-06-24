package com.examruntime_service.examruntime_service.model.dto.cache;

import java.util.List;
import java.util.UUID;

public record ExamPaperPoolDTO(
        UUID examId,
        int snapshotVersion,
        int easyCount,
        int mediumCount,
        int hardCount,
        List<PaperQuestionDTO> questions
) {
}
