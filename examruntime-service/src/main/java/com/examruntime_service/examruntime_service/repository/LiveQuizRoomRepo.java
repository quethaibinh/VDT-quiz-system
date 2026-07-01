package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveQuizRoomRepo extends JpaRepository<LiveQuizRoom, UUID> {
    boolean existsByRoomCode(String roomCode);

    boolean existsByRoomCodeAndStatusIn(String roomCode, List<LiveQuizRoomStatus> statuses);

    Optional<LiveQuizRoom> findByRoomCodeAndStatusIn(String roomCode, List<LiveQuizRoomStatus> statuses);

    Optional<LiveQuizRoom> findByRoomCode(String roomCode);

    List<LiveQuizRoom> findByExamIdAndStatus(UUID examId, LiveQuizRoomStatus status);

    Optional<LiveQuizRoom> findFirstByExamIdAndStatusIn(UUID examId, List<LiveQuizRoomStatus> statuses);
}
