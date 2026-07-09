package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.questions.QuestionDetailResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionUpsertRequestDTO;
import com.question_service.question_service.service.questions.TeacherQuestionCrudService;
import com.question_service.question_service.service.questions.TeacherQuestionSearchService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherQuestionControllerTests {

    private final TeacherQuestionSearchService searchService = mock(TeacherQuestionSearchService.class);
    private final TeacherQuestionCrudService crudService = mock(TeacherQuestionCrudService.class);
    private final TeacherQuestionController controller =
            new TeacherQuestionController(searchService, crudService);

    @Test
    void delegatesCreateUsingAuthenticatedTeacherId() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        QuestionUserPrincipal principal = new QuestionUserPrincipal(
                teacherId.toString(), "teacher", "ROLE_TEACHER"
        );
        QuestionUpsertRequestDTO request = new QuestionUpsertRequestDTO(
                UUID.randomUUID(), "SINGLE_CHOICE", "Question", null, null,
                "EASY", null, null, null, List.of()
        );
        QuestionDetailResponseDTO expected = mock(QuestionDetailResponseDTO.class);
        when(crudService.create(subjectId, teacherId, request)).thenReturn(expected);

        QuestionDetailResponseDTO result = controller.create(subjectId, request, principal);

        assertThat(result).isSameAs(expected);
        verify(crudService).create(subjectId, teacherId, request);
    }
}
