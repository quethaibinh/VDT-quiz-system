package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.LiveQuizEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LiveQuizEventRepo extends JpaRepository<LiveQuizEvent, UUID> {
    List<LiveQuizEvent> findTop100ByRoomIdOrderByOccurredAtDesc(UUID roomId);
}
