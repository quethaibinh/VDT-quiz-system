package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipantPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LiveQuizParticipantPaperRepo extends JpaRepository<LiveQuizParticipantPaper, UUID> {
    Optional<LiveQuizParticipantPaper> findByParticipantId(UUID participantId);

    Optional<LiveQuizParticipantPaper> findByRoomIdAndStudentId(UUID roomId, UUID studentId);
}
