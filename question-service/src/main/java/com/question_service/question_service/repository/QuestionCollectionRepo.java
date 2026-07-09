package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.QuestionCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface QuestionCollectionRepo
        extends JpaRepository<QuestionCollection, UUID>, JpaSpecificationExecutor<QuestionCollection> {

    Optional<QuestionCollection> findByIdAndSubjectId(UUID id, UUID subjectId);

    boolean existsByOwnerTeacherIdAndSubjectIdAndNameIgnoreCase(
            UUID ownerTeacherId,
            UUID subjectId,
            String name
    );

    boolean existsByOwnerTeacherIdAndSubjectIdAndNameIgnoreCaseAndIdNot(
            UUID ownerTeacherId,
            UUID subjectId,
            String name,
            UUID id
    );
}
