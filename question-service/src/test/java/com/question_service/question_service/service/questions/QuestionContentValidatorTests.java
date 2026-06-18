package com.question_service.question_service.service.questions;

import com.question_service.question_service.model.dto.questions.QuestionOptionRequestDTO;
import com.question_service.question_service.model.dto.questions.QuestionUpsertRequestDTO;
import com.question_service.question_service.model.entity.QuestionVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionContentValidatorTests {

    private final QuestionContentValidator validator = new QuestionContentValidator();

    @Test
    void appliesDefaultsAndNormalizesValidSingleChoice() {
        var result = validator.validate(request(
                "single_choice",
                null,
                List.of(option("A", true), option("B", false))
        ));

        assertThat(result.questionType()).isEqualTo("SINGLE_CHOICE");
        assertThat(result.visibility()).isEqualTo(QuestionVisibility.PRIVATE);
        assertThat(result.defaultScore()).isEqualTo(1.0);
        assertThat(result.estimatedSecond()).isZero();
    }

    @Test
    void rejectsDuplicateOptionKeys() {
        assertThatThrownBy(() -> validator.validate(request(
                "SINGLE_CHOICE",
                "PUBLIC",
                List.of(option("A", true), option("a", false))
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("DUPLICATE_OPTION_KEY");
    }

    @Test
    void rejectsInvalidCorrectAnswerCount() {
        assertThatThrownBy(() -> validator.validate(request(
                "MULTI_CHOICE",
                "PUBLIC",
                List.of(option("A", true), option("B", false))
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("INVALID_CORRECT_OPTION_COUNT");
    }

    private QuestionUpsertRequestDTO request(
            String questionType,
            String visibility,
            List<QuestionOptionRequestDTO> options
    ) {
        return new QuestionUpsertRequestDTO(
                UUID.randomUUID(),
                questionType,
                "Question?",
                null,
                null,
                "EASY",
                null,
                null,
                visibility,
                options
        );
    }

    private QuestionOptionRequestDTO option(String key, boolean correct) {
        return new QuestionOptionRequestDTO(key, "Option " + key, null, null, correct);
    }
}
