package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveQuizRoomRepo extends JpaRepository<LiveQuizRoom, UUID> {
    boolean existsByRoomCode(String roomCode);

    boolean existsByRoomCodeAndStatusIn(String roomCode, List<LiveQuizRoomStatus> statuses);

    Optional<LiveQuizRoom> findByRoomCodeAndStatusIn(String roomCode, List<LiveQuizRoomStatus> statuses);

    Optional<LiveQuizRoom> findByRoomCode(String roomCode);

    List<LiveQuizRoom> findByExamIdAndStatus(UUID examId, LiveQuizRoomStatus status);

    List<LiveQuizRoom> findByExamIdInOrderByCreatedAtDesc(List<UUID> examIds);

    Optional<LiveQuizRoom> findFirstByExamIdAndStatusIn(UUID examId, List<LiveQuizRoomStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from LiveQuizRoom room where room.id = :roomId")
    Optional<LiveQuizRoom> findByIdForUpdate(@Param("roomId") UUID roomId);
}
