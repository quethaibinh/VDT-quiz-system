package com.question_service.question_service.service.topics;

import com.question_service.question_service.model.dto.topics.TopicResponseDTO;
import com.question_service.question_service.model.entity.Topic;
import com.question_service.question_service.repository.TopicRepo;
import com.question_service.question_service.service.subjects.TeacherSubjectAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeacherTopicService {

    private final TopicRepo topicRepo;
    private final TeacherSubjectAccessService subjectAccessService;

    public TeacherTopicService(
            TopicRepo topicRepo,
            TeacherSubjectAccessService subjectAccessService
    ) {
        this.topicRepo = topicRepo;
        this.subjectAccessService = subjectAccessService;
    }

    @Transactional(readOnly = true)
    public List<TopicResponseDTO> listTopics(UUID subjectId, UUID teacherId) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        return topicRepo.findBySubjectIdOrderByNameAsc(subjectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TopicResponseDTO toResponse(Topic topic) {
        return new TopicResponseDTO(
                topic.getId(),
                topic.getSubjectId(),
                topic.getName(),
                topic.getDescription(),
                topic.getSlug(),
                topic.getParentTopicId()
        );
    }
}
