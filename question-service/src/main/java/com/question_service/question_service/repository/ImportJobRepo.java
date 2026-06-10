package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportJobRepo extends JpaRepository<ImportJob, UUID> {
}
