package com.examruntime_service.examruntime_service.model.dto.cache;

import java.util.List;
import java.util.UUID;

public record ExamAnswerKeyDTO(
        UUID examId,
        int snapshotVersion,
        List<AnswerEntryDTO> answers
) {
}
