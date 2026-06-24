package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.Submission;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepo extends JpaRepository<Submission, UUID> {

    Optional<Submission> findBySessionId(UUID sessionId);

    Optional<Submission> findByIdempotencyKey(String idempotencyKey);

    Optional<Submission> findByMessageId(UUID messageId);

    List<Submission> findAllByExamIdAndStatus(UUID examId, SubmissionStatus status);
}
