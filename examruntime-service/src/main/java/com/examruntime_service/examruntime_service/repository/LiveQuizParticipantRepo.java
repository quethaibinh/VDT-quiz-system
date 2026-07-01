package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveQuizParticipantRepo extends JpaRepository<LiveQuizParticipant, UUID> {
    Optional<LiveQuizParticipant> findByRoomIdAndStudentId(UUID roomId, UUID studentId);

    List<LiveQuizParticipant> findByRoomIdAndStatus(UUID roomId, LiveQuizParticipantStatus status);

    List<LiveQuizParticipant> findByRoomId(UUID roomId);

    boolean existsByRoomIdAndStudentId(UUID roomId, UUID studentId);
}
