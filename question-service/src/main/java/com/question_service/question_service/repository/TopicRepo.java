package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepo extends JpaRepository<Topic, UUID> {

    Optional<Topic> findBySubjectIdAndNameIgnoreCase(UUID subjectId, String name);

}
