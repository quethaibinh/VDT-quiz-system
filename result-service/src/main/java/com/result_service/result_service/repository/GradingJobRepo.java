package com.result_service.result_service.repository;

import com.result_service.result_service.model.entity.GradingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GradingJobRepo extends JpaRepository<GradingJob, UUID> {
    Optional<GradingJob> findByMessageId(UUID messageId);
}
