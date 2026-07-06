package com.result_service.result_service.model.dto.livequiz;

import java.util.List;

public record TeacherLiveQuizResultDetailDTO(
        TeacherLiveQuizResultRowDTO summary,
        List<LiveQuizResultAnswerDTO> answers
) {
}
