package com.question_service.question_service.service.questions;

import com.question_service.question_service.model.dto.questions.QuestionOptionRequestDTO;
import com.question_service.question_service.model.dto.questions.QuestionUpsertRequestDTO;
import com.question_service.question_service.model.entity.enums.ContentFormat;
import com.question_service.question_service.model.entity.enums.Difficulty;
import com.question_service.question_service.model.entity.enums.OptionKey;
import com.question_service.question_service.model.entity.enums.QuestionVisibility;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class QuestionContentValidator {

    private static final String SINGLE_CHOICE = "SINGLE_CHOICE";
    private static final String MULTI_CHOICE = "MULTI_CHOICE";

    public ValidatedQuestion validate(QuestionUpsertRequestDTO request) {
        if (request == null) {
            throw error("QUESTION_REQUEST_REQUIRED");
        }
        String questionType = normalize(request.questionType());
        if (!Set.of(SINGLE_CHOICE, MULTI_CHOICE).contains(questionType)) {
            throw error("INVALID_QUESTION_TYPE");
        }

        String content = required(request.content(), "CONTENT_REQUIRED");
        ContentFormat contentFormat = parse(
                request.contentFormat(), ContentFormat.class, ContentFormat.PLAIN_TEXT,
                "INVALID_CONTENT_FORMAT"
        );
        Difficulty difficulty = parse(
                request.difficulty(), Difficulty.class, null, "INVALID_DIFFICULTY"
        );
        QuestionVisibility visibility = parse(
                request.visibility(), QuestionVisibility.class, QuestionVisibility.PRIVATE,
                "INVALID_VISIBILITY"
        );
        double defaultScore = request.defaultScore() == null ? 1.0 : request.defaultScore();
        if (defaultScore <= 0) {
            throw error("INVALID_DEFAULT_SCORE");
        }
        int estimatedSecond = request.estimatedSecond() == null ? 0 : request.estimatedSecond();
        if (estimatedSecond < 0) {
            throw error("INVALID_ESTIMATED_SECOND");
        }

        if (request.options() == null || request.options().size() < 2) {
            throw error("NOT_ENOUGH_OPTIONS");
        }
        List<ValidatedOption> options = new ArrayList<>();
        Set<OptionKey> keys = new HashSet<>();
        int correctCount = 0;
        for (QuestionOptionRequestDTO option : request.options()) {
            if (option == null) {
                throw error("INVALID_OPTION");
            }
            OptionKey optionKey = parse(option.optionKey(), OptionKey.class, null, "INVALID_OPTION_KEY");
            if (!keys.add(optionKey)) {
                throw error("DUPLICATE_OPTION_KEY");
            }
            if (option.correct()) {
                correctCount++;
            }
            options.add(new ValidatedOption(
                    optionKey,
                    required(option.content(), "OPTION_CONTENT_REQUIRED"),
                    parse(option.contentFormat(), ContentFormat.class, contentFormat,
                            "INVALID_CONTENT_FORMAT"),
                    trimToNull(option.explanation()),
                    option.correct()
            ));
        }

        if ((SINGLE_CHOICE.equals(questionType) && correctCount != 1)
                || (MULTI_CHOICE.equals(questionType) && correctCount < 2)) {
            throw error("INVALID_CORRECT_OPTION_COUNT");
        }

        return new ValidatedQuestion(
                questionType,
                content,
                contentFormat,
                trimToNull(request.explanation()),
                difficulty,
                defaultScore,
                estimatedSecond,
                visibility,
                List.copyOf(options)
        );
    }

    private <T extends Enum<T>> T parse(
            String value,
            Class<T> type,
            T defaultValue,
            String errorCode
    ) {
        String normalized = normalize(value);
        if (normalized == null) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw error(errorCode);
        }
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException exception) {
            throw error(errorCode);
        }
    }

    private String required(String value, String errorCode) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw error(errorCode);
        }
        return normalized;
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException error(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }

    public record ValidatedQuestion(
            String questionType,
            String content,
            ContentFormat contentFormat,
            String explanation,
            Difficulty difficulty,
            double defaultScore,
            int estimatedSecond,
            QuestionVisibility visibility,
            List<ValidatedOption> options
    ) {
    }

    public record ValidatedOption(
            OptionKey optionKey,
            String content,
            ContentFormat contentFormat,
            String explanation,
            boolean correct
    ) {
    }
}
