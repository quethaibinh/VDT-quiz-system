import type {
  LiveQuizParticipantSnapshot,
  LiveQuizRealtimeMessage,
  LiveQuizTeacherSnapshot,
} from "@/features/teacher/live-quizzes";

export function applyLiveQuizRealtimeMessage(
  snapshot: LiveQuizTeacherSnapshot | undefined,
  message: LiveQuizRealtimeMessage,
): LiveQuizTeacherSnapshot | undefined {
  if (!snapshot || snapshot.roomId !== message.roomId) return snapshot;
  return {
    ...snapshot,
    roomStatus: message.roomStatus ?? snapshot.roomStatus,
    summary: message.summary ?? snapshot.summary,
    leaderboard: message.leaderboard ?? snapshot.leaderboard,
    participants: message.participant
      ? upsertParticipant(snapshot.participants, message.participant)
      : snapshot.participants,
  };
}

function upsertParticipant(
  participants: LiveQuizParticipantSnapshot[],
  participant: LiveQuizParticipantSnapshot,
) {
  const index = participants.findIndex((item) => item.participantId === participant.participantId);
  if (index < 0) {
    return [...participants, participant].sort(compareParticipant);
  }
  const next = [...participants];
  next[index] = participant;
  return next.sort(compareParticipant);
}

function compareParticipant(a: LiveQuizParticipantSnapshot, b: LiveQuizParticipantSnapshot) {
  if (a.currentRank && b.currentRank && a.currentRank !== b.currentRank) return a.currentRank - b.currentRank;
  if (a.status !== b.status) return statusOrder(a.status) - statusOrder(b.status);
  return a.studentName.localeCompare(b.studentName, "vi");
}

function statusOrder(status: LiveQuizParticipantSnapshot["status"]) {
  if (status === "IN_PROGRESS") return 0;
  if (status === "JOINED") return 1;
  if (status === "FINISHED") return 2;
  return 3;
}
