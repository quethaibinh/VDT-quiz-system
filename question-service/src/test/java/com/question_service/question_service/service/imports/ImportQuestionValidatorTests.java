package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionRowDTO;
import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.model.entity.SubjectTeacherStatus;
import com.question_service.question_service.repository.SubjectRepo;
import com.question_service.question_service.repository.SubjectTeacherRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportQuestionValidatorTests {

    private final SubjectRepo subjectRepo = mock(SubjectRepo.class);
    private final SubjectTeacherRepo subjectTeacherRepo = mock(SubjectTeacherRepo.class);
    private final ImportQuestionValidator validator = new ImportQuestionValidator(subjectRepo, subjectTeacherRepo);
    private final UUID subjectId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @Test
    void acceptsValidMultiChoiceQuestionAndNormalizesStatus() {
        allowAssignedActiveSubject();

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                teacherId,
                List.of(row("MULTI_CHOICE", "A,C", "private"))
        );

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().getFirst().getStatus()).isEqualTo("PRIVATE");
        assertThat(result.getRows().getFirst().getCorrectOptionKeys()).hasSize(2);
    }

    @Test
    void rejectsSingleChoiceWithMultipleCorrectOptions() {
        allowAssignedActiveSubject();

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                teacherId,
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
        when(subjectRepo.findById(subjectId)).thenReturn(Optional.empty());

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                teacherId,
                List.of(row("SINGLE_CHOICE", "A", "PUBLIC"))
        );

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().getFirst().getErrorCode()).isEqualTo("SUBJECT_NOT_FOUND");
        assertThat(result.getRows()).isEmpty();
    }

    @Test
    void rejectsStatusOutsidePublicPrivate() {
        allowAssignedActiveSubject();

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                teacherId,
                List.of(row("SINGLE_CHOICE", "A", "ACTIVE"))
        );

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting("errorCode")
                .contains("INVALID_STATUS");
    }

    @Test
    void rejectsArchivedSubjectBeforeRowValidation() {
        Subject subject = subject(SubjectStatus.ARCHIVED);
        when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(subject));

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                teacherId,
                List.of(row("SINGLE_CHOICE", "A", "PUBLIC"))
        );

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().getFirst().getErrorCode()).isEqualTo("SUBJECT_NOT_ACTIVE");
        assertThat(result.getRows()).isEmpty();
    }

    @Test
    void rejectsTeacherWithoutActiveAssignmentBeforeRowValidation() {
        when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(subject(SubjectStatus.ACTIVE)));
        when(subjectTeacherRepo.existsBySubjectIdAndTeacherIdAndStatus(
                subjectId,
                teacherId,
                SubjectTeacherStatus.ACTIVE
        )).thenReturn(false);

        ImportQuestionValidationResult result = validator.validate(
                subjectId,
                teacherId,
                List.of(row("SINGLE_CHOICE", "A", "PUBLIC"))
        );

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().getFirst().getErrorCode()).isEqualTo("SUBJECT_FORBIDDEN");
        assertThat(result.getRows()).isEmpty();
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

    private void allowAssignedActiveSubject() {
        when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(subject(SubjectStatus.ACTIVE)));
        when(subjectTeacherRepo.existsBySubjectIdAndTeacherIdAndStatus(
                subjectId,
                teacherId,
                SubjectTeacherStatus.ACTIVE
        )).thenReturn(true);
    }

    private Subject subject(String status) {
        Subject subject = new Subject();
        subject.setStatus(status);
        return subject;
    }

}
