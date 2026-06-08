package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.imports.ImportQuestionResultDTO;
import com.question_service.question_service.service.imports.ImportQuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportQuestionControllerTests {

    private final ImportQuestionService importQuestionService = mock(ImportQuestionService.class);
    private final ImportQuestionController controller = new ImportQuestionController(importQuestionService);
    private final MockMultipartFile file = new MockMultipartFile(
            "file",
            "questions.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1}
    );

    @Test
    void delegatesImportToServiceWithPrincipalTeacherId() throws Exception {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        QuestionUserPrincipal principal = new QuestionUserPrincipal(
                teacherId.toString(),
                "teacher01",
                "ROLE_TEACHER"
        );
        ImportQuestionResultDTO expected = new ImportQuestionResultDTO();

        when(importQuestionService.importQuestions(subjectId, teacherId, file)).thenReturn(expected);

        ImportQuestionResultDTO result = controller.importQuestions(subjectId, file, principal);

        assertThat(result).isSameAs(expected);
        verify(importQuestionService).importQuestions(subjectId, teacherId, file);
    }

}
