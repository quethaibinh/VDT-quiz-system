import type { LiveQuizRankEntry } from "@/features/live-quizzes/components/live-quiz-leaderboard";
import type { LiveQuizLeaderboardEntry } from "@/features/teacher/live-quizzes";

export function mapLeaderboardEntries(
  entries: LiveQuizLeaderboardEntry[],
  totalQuestions?: number,
  selectedParticipantId?: string | null,
): LiveQuizRankEntry[] {
  return entries.map((entry) => ({
    id: entry.participantId,
    rank: entry.rank,
    name: entry.studentName,
    score: entry.totalScore,
    answeredCount: entry.answeredCount,
    totalQuestions,
    correctCount: entry.correctCount,
    timeoutCount: entry.timeoutCount,
    averageResponseMs: entry.averageResponseMs,
    statusLabel: entry.finished ? "FINISHED" : "LIVE",
    statusTone: entry.finished ? "success" : "warning",
    selected: selectedParticipantId === entry.participantId,
  }));
}
