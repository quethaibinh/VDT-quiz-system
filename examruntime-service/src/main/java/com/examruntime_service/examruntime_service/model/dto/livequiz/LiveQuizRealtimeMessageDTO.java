package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LiveQuizRealtimeMessageDTO(
        UUID roomId,
        UUID examId,
        String type,
        OffsetDateTime occurredAt,
        LiveQuizRoomStatus roomStatus,
        UUID studentTargetId,
        LiveQuizParticipantSnapshotDTO participant,
        List<LiveQuizLeaderboardEntryDTO> leaderboard,
        LiveQuizTeacherSnapshotDTO.Summary summary
) {
}
