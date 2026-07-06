package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.LiveQuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveQuizAnswerRepo extends JpaRepository<LiveQuizAnswer, UUID> {
    Optional<LiveQuizAnswer> findByRoomIdAndParticipantIdAndQuestionId(UUID roomId, UUID participantId, UUID questionId);

    List<LiveQuizAnswer> findByRoomIdAndParticipantIdOrderByQuestionPositionAsc(UUID roomId, UUID participantId);
}
