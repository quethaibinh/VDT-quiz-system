package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizLeaderboardEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizParticipantSnapshotDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizTeacherSnapshotDTO;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class LiveQuizTeacherSnapshotService {

    private final LiveQuizRoomService roomService;
    private final LiveQuizParticipantRepo participantRepo;
    private final Clock clock;

    /**
     * Inject room service va participant repo de build snapshot cho teacher.
     */
    public LiveQuizTeacherSnapshotService(
            LiveQuizRoomService roomService,
            LiveQuizParticipantRepo participantRepo,
            Clock clock
    ) {
        this.roomService = roomService;
        this.participantRepo = participantRepo;
        this.clock = clock;
    }

    /**
     * Lay snapshot teacher sau khi check teacher so huu room.
     */
    @Transactional(readOnly = true)
    public LiveQuizTeacherSnapshotDTO snapshot(UUID roomId, UUID teacherId) {
        LiveQuizRoom room = roomService.requireOwned(roomId, teacherId);
        return snapshot(room);
    }

    /**
     * Tao snapshot day du cua room: summary, participant list va leaderboard.
     */
    @Transactional(readOnly = true)
    public LiveQuizTeacherSnapshotDTO snapshot(LiveQuizRoom room) {
        List<LiveQuizParticipant> participants = participantRepo.findByRoomId(room.getId());
        List<LiveQuizParticipant> rankedParticipants = rankedParticipants(participants);
        List<LiveQuizParticipantSnapshotDTO> participantDtos = participants.stream()
                .map(participant -> toParticipantSnapshot(participant, rankOf(rankedParticipants, participant.getId())))
                .toList();
        List<LiveQuizLeaderboardEntryDTO> leaderboard = leaderboard(rankedParticipants);
        return new LiveQuizTeacherSnapshotDTO(
                room.getId(),
                room.getExamId(),
                room.getRoomCode(),
                room.getQuizTitle(),
                room.getSubjectName(),
                room.getQuestionCount(),
                room.isShowLeaderboard(),
                room.getStatus(),
                OffsetDateTime.now(clock),
                summary(participants),
                participantDtos,
                leaderboard
        );
    }

    /**
     * Sap xep participant thanh bang xep hang hien tai cua room.
     */
    public List<LiveQuizLeaderboardEntryDTO> leaderboard(List<LiveQuizParticipant> participants) {
        List<LiveQuizParticipant> sorted = rankedParticipants(participants);
        List<LiveQuizLeaderboardEntryDTO> result = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            LiveQuizParticipant participant = sorted.get(index);
            result.add(new LiveQuizLeaderboardEntryDTO(
                    index + 1,
                    participant.getId(),
                    participant.getStudentId(),
                    participant.getStudentNameSnapshot(),
                    participant.getTotalScore(),
                    participant.getAnsweredCount(),
                    participant.getCorrectCount(),
                    participant.getTimeoutCount(),
                    participant.getAverageResponseMs(),
                    participant.getStatus() == LiveQuizParticipantStatus.FINISHED
            ));
        }
        return result;
    }

    /**
     * Map participant entity sang snapshot DTO gui cho teacher/realtime.
     */
    public LiveQuizParticipantSnapshotDTO toParticipantSnapshot(LiveQuizParticipant participant) {
        return toParticipantSnapshot(participant, participant.getCurrentRank());
    }

    public Integer currentRank(UUID roomId, UUID participantId) {
        return rankOf(rankedParticipants(participantRepo.findByRoomId(roomId)), participantId);
    }

    /**
     * Map participant entity sang snapshot DTO voi rank da tinh tu danh sach hien tai.
     */
    public LiveQuizParticipantSnapshotDTO toParticipantSnapshot(LiveQuizParticipant participant, Integer currentRank) {
        return new LiveQuizParticipantSnapshotDTO(
                participant.getId(),
                participant.getStudentId(),
                participant.getStudentCodeSnapshot(),
                participant.getStudentNameSnapshot(),
                participant.getStatus(),
                participant.getAnsweredCount(),
                participant.getTotalQuestions(),
                participant.getCorrectCount(),
                participant.getWrongCount(),
                participant.getTimeoutCount(),
                participant.getTotalScore(),
                participant.getMaxScore(),
                participant.getAverageResponseMs(),
                currentRank,
                participant.getJoinedAt(),
                participant.getStartedAt(),
                participant.getFinishedAt(),
                participant.getLastSeenAt()
        );
    }

    private List<LiveQuizParticipant> rankedParticipants(List<LiveQuizParticipant> participants) {
        List<LiveQuizParticipant> sorted = new ArrayList<>(participants);
        sorted.sort(leaderboardComparator());
        return sorted;
    }

    private Integer rankOf(List<LiveQuizParticipant> rankedParticipants, UUID participantId) {
        for (int index = 0; index < rankedParticipants.size(); index++) {
            if (rankedParticipants.get(index).getId().equals(participantId)) {
                return index + 1;
            }
        }
        return null;
    }

    /**
     * Dem so participant theo tung trang thai de teacher UI hien tong quan.
     */
    public LiveQuizTeacherSnapshotDTO.Summary summary(List<LiveQuizParticipant> participants) {
        int joined = 0;
        int inProgress = 0;
        int finished = 0;
        int disconnected = 0;
        for (LiveQuizParticipant participant : participants) {
            if (participant.getStatus() == LiveQuizParticipantStatus.JOINED) {
                joined++;
            } else if (participant.getStatus() == LiveQuizParticipantStatus.IN_PROGRESS) {
                inProgress++;
            } else if (participant.getStatus() == LiveQuizParticipantStatus.FINISHED) {
                finished++;
            } else if (participant.getStatus() == LiveQuizParticipantStatus.DISCONNECTED) {
                disconnected++;
            }
        }
        return new LiveQuizTeacherSnapshotDTO.Summary(joined, inProgress, finished, disconnected);
    }

    /**
     * Dinh nghia thu tu xep hang: diem, so cau tra loi, so cau dung, toc do, thoi diem.
     */
    private Comparator<LiveQuizParticipant> leaderboardComparator() {
        return Comparator
                .comparing(LiveQuizParticipant::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LiveQuizParticipant::getAnsweredCount, Comparator.reverseOrder())
                .thenComparing(LiveQuizParticipant::getCorrectCount, Comparator.reverseOrder())
                .thenComparing(LiveQuizParticipant::getAverageResponseMs, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LiveQuizParticipant::getFinishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LiveQuizParticipant::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
