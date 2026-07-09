package com.question_service.question_service.service.topics;

import com.question_service.question_service.model.entity.Topic;
import com.question_service.question_service.repository.TopicRepo;
import com.question_service.question_service.service.subjects.TeacherSubjectAccessService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherTopicServiceTests {

    private final TopicRepo topicRepo = mock(TopicRepo.class);
    private final TeacherSubjectAccessService accessService = mock(TeacherSubjectAccessService.class);
    private final TeacherTopicService service = new TeacherTopicService(topicRepo, accessService);

    @Test
    void checksAssignmentAndMapsTopicsToResponseDtos() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setSubjectId(subjectId);
        topic.setName("Algebra");
        topic.setDescription("Basic algebra");
        topic.setSlug("algebra");

        when(topicRepo.findBySubjectIdOrderByNameAsc(subjectId)).thenReturn(List.of(topic));

        var result = service.listTopics(subjectId, teacherId);

        verify(accessService).requireActiveAssignment(subjectId, teacherId);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(topic.getId());
        assertThat(result.getFirst().name()).isEqualTo("Algebra");
    }
}
