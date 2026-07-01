package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LiveQuizTeacherSnapshotDTO(
        UUID roomId,
        UUID examId,
        String roomCode,
        LiveQuizRoomStatus roomStatus,
        OffsetDateTime serverTime,
        Summary summary,
        List<LiveQuizParticipantSnapshotDTO> participants,
        List<LiveQuizLeaderboardEntryDTO> leaderboard
) {
    public record Summary(
            int joined,
            int inProgress,
            int finished,
            int disconnected
    ) {
    }
}
