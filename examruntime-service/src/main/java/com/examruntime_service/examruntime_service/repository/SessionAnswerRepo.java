package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionAnswerRepo extends JpaRepository<SessionAnswer, UUID> {

    List<SessionAnswer> findAllBySessionId(UUID sessionId);

    Optional<SessionAnswer> findBySessionIdAndQuestionId(UUID sessionId, UUID questionId);
}
