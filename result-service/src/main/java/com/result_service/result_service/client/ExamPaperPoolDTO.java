package com.result_service.result_service.client;

import java.util.List;
import java.util.UUID;

public record ExamPaperPoolDTO(
        UUID examId,
        int snapshotVersion,
        List<PaperQuestionDTO> questions
) {
}
