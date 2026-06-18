package com.question_service.question_service.controller;

import com.question_service.question_service.config.security.QuestionUserPrincipal;
import com.question_service.question_service.model.dto.topics.TopicResponseDTO;
import com.question_service.question_service.service.subjects.SubjectTeacherService;
import com.question_service.question_service.service.topics.TeacherTopicService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherSubjectControllerTests {

    private final SubjectTeacherService subjectTeacherService = mock(SubjectTeacherService.class);
    private final TeacherTopicService topicService = mock(TeacherTopicService.class);
    private final TeacherSubjectController controller =
            new TeacherSubjectController(subjectTeacherService, topicService);

    @Test
    void delegatesTopicListingToServiceWithPrincipalTeacherId() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        QuestionUserPrincipal principal = new QuestionUserPrincipal(
                teacherId.toString(), "teacher", "ROLE_TEACHER"
        );
        List<TopicResponseDTO> expected = List.of(new TopicResponseDTO(
                UUID.randomUUID(), subjectId, "Algebra", null, "algebra", null
        ));
        when(topicService.listTopics(subjectId, teacherId)).thenReturn(expected);

        var result = controller.listTopics(subjectId, principal);

        assertThat(result).isSameAs(expected);
        verify(topicService).listTopics(subjectId, teacherId);
    }
}
