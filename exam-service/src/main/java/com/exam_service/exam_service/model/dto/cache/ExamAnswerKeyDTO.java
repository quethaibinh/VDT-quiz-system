package com.exam_service.exam_service.model.dto.cache;

import java.util.List;
import java.util.UUID;

public record ExamAnswerKeyDTO(
        UUID examId,
        int snapshotVersion,
        List<AnswerEntryDTO> answers
) {
}
