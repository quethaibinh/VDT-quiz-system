package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepo extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    Optional<Question> findByIdAndSubjectId(UUID id, UUID subjectId);

}
