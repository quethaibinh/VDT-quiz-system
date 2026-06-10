package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionErrorDTO;
import com.question_service.question_service.model.dto.imports.ImportQuestionResultDTO;
import com.question_service.question_service.model.dto.imports.ImportQuestionRowDTO;
import com.question_service.question_service.model.dto.imports.NormalizedImportQuestionRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportQuestionServiceTests {

    private final ExcelQuestionParser parser = mock(ExcelQuestionParser.class);
    private final ImportQuestionValidator validator = mock(ImportQuestionValidator.class);
    private final ImportedQuestionCreationService creationService = mock(ImportedQuestionCreationService.class);
    private final ImportQuestionService service = new ImportQuestionService(parser, validator, creationService);
    private final UUID subjectId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final MockMultipartFile file = new MockMultipartFile(
            "file",
            "questions.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1}
    );

    @Test
    void returnsValidationErrorsAndDoesNotCreateAnything() throws Exception {
        List<ImportQuestionRowDTO> rows = List.of(new ImportQuestionRowDTO());
        ImportQuestionValidationResult validationResult = new ImportQuestionValidationResult();
        validationResult.addError(new ImportQuestionErrorDTO(
                2,
                "status",
                "INVALID_STATUS",
                "Status must be PUBLIC or PRIVATE"
        ));

        when(validator.validateAccess(subjectId, teacherId)).thenReturn(new ImportQuestionValidationResult());
        when(parser.parse(file)).thenReturn(rows);
        when(validator.validateRows(rows)).thenReturn(validationResult);

        ImportQuestionResultDTO result = service.importQuestions(subjectId, teacherId, file);

        assertThat(result.isImported()).isFalse();
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        verify(creationService, never()).createQuestions(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void createsQuestionsAfterValidationPasses() throws Exception {
        List<ImportQuestionRowDTO> rows = List.of(new ImportQuestionRowDTO());
        ImportQuestionValidationResult validationResult = new ImportQuestionValidationResult();
        validationResult.setRows(List.of(new NormalizedImportQuestionRow()));
        ImportedQuestionCreationResult creationResult = new ImportedQuestionCreationResult(
                UUID.randomUUID(),
                1,
                1
        );

        when(validator.validateAccess(subjectId, teacherId)).thenReturn(new ImportQuestionValidationResult());
        when(parser.parse(file)).thenReturn(rows);
        when(validator.validateRows(rows)).thenReturn(validationResult);
        when(creationService.createQuestions(subjectId, teacherId, file, validationResult.getRows()))
                .thenReturn(creationResult);

        ImportQuestionResultDTO result = service.importQuestions(subjectId, teacherId, file);

        assertThat(result.isImported()).isTrue();
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getCreatedTopicCount()).isEqualTo(1);
        assertThat(result.getCreatedQuestionCount()).isEqualTo(1);
        verify(creationService).createQuestions(subjectId, teacherId, file, validationResult.getRows());
    }

    @Test
    void rejectsFileWithNoQuestionRows() throws Exception {
        when(validator.validateAccess(subjectId, teacherId)).thenReturn(new ImportQuestionValidationResult());
        when(parser.parse(file)).thenReturn(List.of());

        ImportQuestionResultDTO result = service.importQuestions(subjectId, teacherId, file);

        assertThat(result.isImported()).isFalse();
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getErrors().getFirst().getErrorCode()).isEqualTo("NO_ROWS");
        verify(validator, never()).validate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(validator, never()).validateRows(org.mockito.ArgumentMatchers.any());
        verify(creationService, never()).createQuestions(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectsSubjectAccessBeforeParsingFile() throws Exception {
        ImportQuestionValidationResult accessResult = new ImportQuestionValidationResult();
        accessResult.addError(new ImportQuestionErrorDTO(
                0,
                "subjectId",
                "SUBJECT_FORBIDDEN",
                "Teacher is not assigned to this subject"
        ));

        when(validator.validateAccess(subjectId, teacherId)).thenReturn(accessResult);

        ImportQuestionResultDTO result = service.importQuestions(subjectId, teacherId, file);

        assertThat(result.isImported()).isFalse();
        assertThat(result.getErrors().getFirst().getErrorCode()).isEqualTo("SUBJECT_FORBIDDEN");
        verify(parser, never()).parse(file);
        verify(creationService, never()).createQuestions(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

}
