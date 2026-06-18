package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionErrorDTO;
import com.question_service.question_service.model.dto.imports.ImportQuestionRowDTO;
import com.question_service.question_service.model.dto.imports.NormalizedImportQuestionRow;
import com.question_service.question_service.model.dto.imports.NormalizedQuestionOptionDTO;
import com.question_service.question_service.model.entity.ContentFormat;
import com.question_service.question_service.model.entity.Difficulty;
import com.question_service.question_service.model.entity.OptionKey;
import com.question_service.question_service.model.entity.QuestionVisibility;
import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.model.entity.SubjectTeacherStatus;
import com.question_service.question_service.repository.SubjectRepo;
import com.question_service.question_service.repository.SubjectTeacherRepo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
/**
 * Kiem tra quyen mon hoc va chuan hoa tung dong import cau hoi.
 */
public class ImportQuestionValidator {

    private static final String SINGLE_CHOICE = "SINGLE_CHOICE";
    private static final String MULTI_CHOICE = "MULTI_CHOICE";
    private final SubjectRepo subjectRepo;
    private final SubjectTeacherRepo subjectTeacherRepo;

    public ImportQuestionValidator(SubjectRepo subjectRepo, SubjectTeacherRepo subjectTeacherRepo) {
        this.subjectRepo = subjectRepo;
        this.subjectTeacherRepo = subjectTeacherRepo;
    }

    public ImportQuestionValidationResult validate(UUID subjectId, UUID teacherId, List<ImportQuestionRowDTO> rows) {
        ImportQuestionValidationResult result = new ImportQuestionValidationResult();

        ImportQuestionValidationResult accessResult = validateAccess(subjectId, teacherId);
        if (accessResult.hasErrors()) {
            return accessResult;
        }

        return validateRows(rows);
    }

    /**
     * Xac minh giao vien duoc phan cong vao mon hoc dang hoat dong.
     */
    public ImportQuestionValidationResult validateAccess(UUID subjectId, UUID teacherId) {
        ImportQuestionValidationResult result = new ImportQuestionValidationResult();

        if (subjectId == null) {
            result.addError(new ImportQuestionErrorDTO(0, "subjectId", "SUBJECT_NOT_FOUND", "Subject does not exist"));
            return result;
        }

        Subject subject = subjectRepo.findById(subjectId).orElse(null);
        if (subject == null) {
            result.addError(new ImportQuestionErrorDTO(0, "subjectId", "SUBJECT_NOT_FOUND", "Subject does not exist"));
            return result;
        }

        if (!SubjectStatus.ACTIVE.equals(subject.getStatus())) {
            result.addError(new ImportQuestionErrorDTO(
                    0,
                    "subjectId",
                    "SUBJECT_NOT_ACTIVE",
                    "Subject is not active"
            ));
            return result;
        }

        if (teacherId == null || !subjectTeacherRepo.existsBySubjectIdAndTeacherIdAndStatus(
                subjectId,
                teacherId,
                SubjectTeacherStatus.ACTIVE
        )) {
            result.addError(new ImportQuestionErrorDTO(
                    0,
                    "subjectId",
                    "SUBJECT_FORBIDDEN",
                    "Teacher is not assigned to this subject"
            ));
            return result;
        }

        return result;
    }

    /**
     * Thu thap tat ca loi de nguoi dung sua tep trong mot lan.
     */
    public ImportQuestionValidationResult validateRows(List<ImportQuestionRowDTO> rows) {
        ImportQuestionValidationResult result = new ImportQuestionValidationResult();

        for (ImportQuestionRowDTO row : rows) {
            validateRow(row, result);
        }

        return result;
    }

    private void validateRow(ImportQuestionRowDTO row, ImportQuestionValidationResult result) {
        int errorCountBefore = result.getErrors().size();

        require(row, result, "topicName", row.getTopicName());
        require(row, result, "questionType", row.getQuestionType());
        require(row, result, "content", row.getContent());
        require(row, result, "optionA", row.getOptionA());
        require(row, result, "optionB", row.getOptionB());
        require(row, result, "correctOptions", row.getCorrectOptions());
        require(row, result, "difficulty", row.getDifficulty());
        require(row, result, "visibility", row.getVisibility());

        String questionType = normalize(row.getQuestionType());
        if (!isBlank(row.getQuestionType())
                && !SINGLE_CHOICE.equals(questionType)
                && !MULTI_CHOICE.equals(questionType)) {
            addError(row, result, "questionType", "INVALID_QUESTION_TYPE",
                    "Question type must be SINGLE_CHOICE or MULTI_CHOICE");
        }

        List<NormalizedQuestionOptionDTO> options = getOptions(row);
        if (options.size() < 2) {
            addError(row, result, "options", "NOT_ENOUGH_OPTIONS",
                    "At least two options are required");
        }

        Set<OptionKey> correctOptionKeys = parseCorrectOptions(row, result);
        Set<OptionKey> optionKeys = new LinkedHashSet<>();
        for (NormalizedQuestionOptionDTO option : options) {
            optionKeys.add(option.getOptionKey());
        }

        for (OptionKey correctOptionKey : correctOptionKeys) {
            if (!optionKeys.contains(correctOptionKey)) {
                addError(row, result, "correctOptions", "INVALID_CORRECT_OPTION",
                        "Correct option " + correctOptionKey + " has no option content");
            }
        }

        if (SINGLE_CHOICE.equals(questionType) && correctOptionKeys.size() != 1) {
            addError(row, result, "correctOptions", "INVALID_SINGLE_CHOICE_CORRECT_COUNT",
                    "Single choice question must have exactly one correct option");
        }

        if (MULTI_CHOICE.equals(questionType) && correctOptionKeys.size() < 2) {
            addError(row, result, "correctOptions", "INVALID_MULTI_CHOICE_CORRECT_COUNT",
                    "Multi choice question must have at least two correct options");
        }

        Difficulty difficulty = parseEnum(row, result, "difficulty", row.getDifficulty(),
                Difficulty.class, "INVALID_DIFFICULTY", "Difficulty must be EASY, MEDIUM, or HARD");
        ContentFormat contentFormat = isBlank(row.getContentFormat())
                ? ContentFormat.PLAIN_TEXT
                : parseEnum(row, result, "contentFormat", row.getContentFormat(),
                ContentFormat.class, "INVALID_CONTENT_FORMAT",
                "Content format must be PLAIN_TEXT, MARKDOWN, or HTML_SAFE");
        Double defaultScore = parseDefaultScore(row, result);
        Integer estimatedSecond = parseEstimatedSecond(row, result);
        QuestionVisibility visibility = parseEnum(row, result, "visibility", row.getVisibility(),
                QuestionVisibility.class, "INVALID_VISIBILITY",
                "Visibility must be PUBLIC or PRIVATE");

        if (result.getErrors().size() == errorCountBefore) {
            // Chi tao dong chuan hoa khi dong goc khong co bat ky loi nao.
            result.getRows().add(new NormalizedImportQuestionRow(
                    row.getRowNumber(),
                    row.getTopicName().trim(),
                    questionType,
                    row.getContent().trim(),
                    options,
                    correctOptionKeys,
                    difficulty,
                    defaultScore,
                    estimatedSecond,
                    trimToNull(row.getExplanation()),
                    visibility,
                    contentFormat
            ));
        }
    }

    private void require(
            ImportQuestionRowDTO row,
            ImportQuestionValidationResult result,
            String fieldName,
            String value
    ) {
        if (isBlank(value)) {
            addError(row, result, fieldName, "REQUIRED", fieldName + " is required");
        }
    }

    private List<NormalizedQuestionOptionDTO> getOptions(ImportQuestionRowDTO row) {
        List<NormalizedQuestionOptionDTO> options = new ArrayList<>();
        addOption(options, OptionKey.A, row.getOptionA());
        addOption(options, OptionKey.B, row.getOptionB());
        addOption(options, OptionKey.C, row.getOptionC());
        addOption(options, OptionKey.D, row.getOptionD());
        addOption(options, OptionKey.E, row.getOptionE());
        addOption(options, OptionKey.F, row.getOptionF());
        addOption(options, OptionKey.G, row.getOptionG());
        return options;
    }

    private void addOption(List<NormalizedQuestionOptionDTO> options, OptionKey optionKey, String content) {
        String normalizedContent = trimToNull(content);
        if (normalizedContent != null) {
            options.add(new NormalizedQuestionOptionDTO(optionKey, normalizedContent));
        }
    }

    private Set<OptionKey> parseCorrectOptions(
            ImportQuestionRowDTO row,
            ImportQuestionValidationResult result
    ) {
        Set<OptionKey> correctOptionKeys = new LinkedHashSet<>();
        if (isBlank(row.getCorrectOptions())) {
            return correctOptionKeys;
        }

        String[] values = row.getCorrectOptions().split(",");
        for (String value : values) {
            String option = normalize(value);
            if (option == null) {
                continue;
            }

            try {
                correctOptionKeys.add(OptionKey.valueOf(option));
            } catch (IllegalArgumentException exception) {
                addError(row, result, "correctOptions", "INVALID_CORRECT_OPTION",
                        "Correct option must be one of A, B, C, D, E, F, G");
            }
        }

        return correctOptionKeys;
    }

    private Double parseDefaultScore(ImportQuestionRowDTO row, ImportQuestionValidationResult result) {
        if (isBlank(row.getDefaultScore())) {
            return 1.0;
        }

        try {
            double score = Double.parseDouble(row.getDefaultScore());
            if (score <= 0) {
                addError(row, result, "defaultScore", "INVALID_DEFAULT_SCORE",
                        "Default score must be greater than 0");
            }
            return score;
        } catch (NumberFormatException exception) {
            addError(row, result, "defaultScore", "INVALID_DEFAULT_SCORE",
                    "Default score must be a number");
            return 1.0;
        }
    }

    private Integer parseEstimatedSecond(ImportQuestionRowDTO row, ImportQuestionValidationResult result) {
        if (isBlank(row.getEstimatedSecond())) {
            return 0;
        }

        try {
            int seconds = Integer.parseInt(row.getEstimatedSecond());
            if (seconds < 0) {
                addError(row, result, "estimatedSecond", "INVALID_ESTIMATED_SECOND",
                        "Estimated second must be greater than or equal to 0");
            }
            return seconds;
        } catch (NumberFormatException exception) {
            addError(row, result, "estimatedSecond", "INVALID_ESTIMATED_SECOND",
                    "Estimated second must be an integer");
            return 0;
        }
    }

    private <T extends Enum<T>> T parseEnum(
            ImportQuestionRowDTO row,
            ImportQuestionValidationResult result,
            String fieldName,
            String value,
            Class<T> enumType,
            String errorCode,
            String message
    ) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, normalize(value));
        } catch (IllegalArgumentException exception) {
            addError(row, result, fieldName, errorCode, message);
            return null;
        }
    }

    private void addError(
            ImportQuestionRowDTO row,
            ImportQuestionValidationResult result,
            String fieldName,
            String errorCode,
            String message
    ) {
        result.addError(new ImportQuestionErrorDTO(row.getRowNumber(), fieldName, errorCode, message));
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
