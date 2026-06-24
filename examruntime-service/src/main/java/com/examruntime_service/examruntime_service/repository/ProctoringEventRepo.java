package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.ProctoringEvent;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProctoringEventRepo extends JpaRepository<ProctoringEvent, UUID> {

    Optional<ProctoringEvent> findBySessionIdAndClientEventId(UUID sessionId, String clientEventId);

    List<ProctoringEvent> findAllByExamIdOrderByOccurredAtDesc(UUID examId, Pageable pageable);

    List<ProctoringEvent> findAllBySessionIdAndEventTypeOrderByOccurredAtDesc(
            UUID sessionId,
            ProctoringEventType eventType,
            Pageable pageable
    );
}
