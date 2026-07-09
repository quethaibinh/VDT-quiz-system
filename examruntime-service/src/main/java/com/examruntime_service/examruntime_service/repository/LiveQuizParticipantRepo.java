package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveQuizParticipantRepo extends JpaRepository<LiveQuizParticipant, UUID> {
    Optional<LiveQuizParticipant> findByRoomIdAndStudentId(UUID roomId, UUID studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select participant
            from LiveQuizParticipant participant
            where participant.roomId = :roomId
              and participant.studentId = :studentId
            """)
    Optional<LiveQuizParticipant> findByRoomIdAndStudentIdForUpdate(
            @Param("roomId") UUID roomId,
            @Param("studentId") UUID studentId
    );

    List<LiveQuizParticipant> findByRoomIdAndStatus(UUID roomId, LiveQuizParticipantStatus status);

    List<LiveQuizParticipant> findByRoomId(UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select participant
            from LiveQuizParticipant participant
            where participant.roomId = :roomId
            """)
    List<LiveQuizParticipant> findByRoomIdForUpdate(@Param("roomId") UUID roomId);

    boolean existsByRoomIdAndStudentId(UUID roomId, UUID studentId);
}
