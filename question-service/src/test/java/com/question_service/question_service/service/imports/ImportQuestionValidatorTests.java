package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionRowDTO;
import com.question_service.question_service.repository.SubjectRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportQuestionValidatorTests {

    private final SubjectRepo subjectRepo = mock(SubjectRepo.class);
    private final ImportQuestionValidator validator = new ImportQuestionValidator(subjectRepo);
    private final UUID subjectId = UUID.randomUUID();

    @Test
    void acceptsValidMultiChoiceQuestionAndNormalizesStatus() {
        when(subjectRepo.existsById(subjectId)).thenReturn(true);

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                List.of(row("MULTI_CHOICE", "A,C", "private"))
        );

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().getFirst().getStatus()).isEqualTo("PRIVATE");
        assertThat(result.getRows().getFirst().getCorrectOptionKeys()).hasSize(2);
    }

    @Test
    void rejectsSingleChoiceWithMultipleCorrectOptions() {
        when(subjectRepo.existsById(subjectId)).thenReturn(true);

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                List.of(row("SINGLE_CHOICE", "A,C", "PUBLIC"))
        );

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting("errorCode")
                .contains("INVALID_SINGLE_CHOICE_CORRECT_COUNT");
        assertThat(result.getRows()).isEmpty();
    }

    @Test
    void rejectsMissingSubjectBeforeRowValidation() {
        when(subjectRepo.existsById(subjectId)).thenReturn(false);

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                List.of(row("SINGLE_CHOICE", "A", "PUBLIC"))
        );

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().getFirst().getErrorCode()).isEqualTo("SUBJECT_NOT_FOUND");
        assertThat(result.getRows()).isEmpty();
    }

    @Test
    void rejectsStatusOutsidePublicPrivate() {
        when(subjectRepo.existsById(subjectId)).thenReturn(true);

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                List.of(row("SINGLE_CHOICE", "A", "ACTIVE"))
        );

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting("errorCode")
                .contains("INVALID_STATUS");
    }

    private ImportQuestionRowDTO row(String questionType, String correctOptions, String status) {
        return new ImportQuestionRowDTO(
                2,
                "Algebra",
                questionType,
                "1 + 1 = ?",
                "2",
                "3",
                "4",
                null,
                null,
                null,
                null,
                correctOptions,
                "EASY",
                null,
                null,
                null,
                status,
                null
        );
    }

}
