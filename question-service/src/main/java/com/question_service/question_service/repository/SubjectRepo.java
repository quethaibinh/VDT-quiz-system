package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepo extends JpaRepository<Subject, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    Optional<Subject> findByIdAndStatus(UUID id, String status);

    List<Subject> findByStatus(String status);

}
