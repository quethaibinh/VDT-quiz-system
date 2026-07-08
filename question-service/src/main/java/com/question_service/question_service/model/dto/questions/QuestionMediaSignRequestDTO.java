package com.question_service.question_service.model.dto.questions;

import java.util.List;

public record QuestionMediaSignRequestDTO(
        List<String> objectKeys
) {
}
