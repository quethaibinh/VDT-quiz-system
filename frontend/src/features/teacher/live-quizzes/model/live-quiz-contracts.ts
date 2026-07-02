export type LiveQuizStatus = "DRAFT" | "PREPARED";
export type LiveQuizJoinPolicy = "CODE_ONLY";
export type LiveQuizRoomStatus = "PREPARING" | "OPEN" | "STARTED" | "CLOSED";

export interface LiveQuizSummary {
  id: string;
  code: string;
  title: string;
  subjectId: string;
  subjectName: string;
  collectionId: string;
  collectionName: string;
  questionCount: number;
  shuffleQuestions: boolean;
  showLeaderboard: boolean;
  showCorrectAnswer: boolean;
  joinPolicy: LiveQuizJoinPolicy;
  status: LiveQuizStatus;
  snapshotVersion: number;
  version: number;
}

export interface LiveQuizDetail extends LiveQuizSummary {
  description: string | null;
  preparedAt: string | null;
}

export interface LiveQuizRequest {
  title: string;
  description: string | null;
  collectionId: string;
  shuffleQuestions: boolean;
  showLeaderboard: boolean;
  showCorrectAnswer: boolean;
  joinPolicy: LiveQuizJoinPolicy;
}

export interface LiveQuizPrepareResponse {
  quiz: LiveQuizDetail;
  roomId: string;
  roomCode: string;
  roomStatus: LiveQuizRoomStatus;
}

export interface LiveQuizRoom {
  roomId: string;
  examId: string;
  roomCode: string;
  quizTitle: string;
  subjectName: string;
  questionCount: number;
  showLeaderboard: boolean;
  ownerTeacherId: string;
  status: LiveQuizRoomStatus;
  openedAt: string | null;
  startedAt: string | null;
  closedAt: string | null;
}

export type LiveQuizParticipantStatus = "JOINED" | "IN_PROGRESS" | "FINISHED" | "DISCONNECTED";

export interface LiveQuizParticipantSnapshot {
  participantId: string;
  studentId: string;
  studentCode: string | null;
  studentName: string;
  status: LiveQuizParticipantStatus;
  answeredCount: number;
  totalQuestions: number;
  correctCount: number;
  wrongCount: number;
  timeoutCount: number;
  totalScore: number;
  maxScore: number;
  averageResponseMs: number | null;
  currentRank: number | null;
  joinedAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  lastSeenAt: string | null;
}

export interface LiveQuizLeaderboardEntry {
  rank: number;
  participantId: string;
  studentId: string;
  studentName: string;
  totalScore: number;
  answeredCount: number;
  correctCount: number;
  timeoutCount: number;
  averageResponseMs: number | null;
  finished: boolean;
}

export interface LiveQuizTeacherSummary {
  joined: number;
  inProgress: number;
  finished: number;
  disconnected: number;
}

export interface LiveQuizTeacherSnapshot {
  roomId: string;
  examId: string;
  roomCode: string;
  quizTitle: string;
  subjectName: string;
  questionCount: number;
  showLeaderboard: boolean;
  roomStatus: LiveQuizRoomStatus;
  serverTime: string;
  summary: LiveQuizTeacherSummary;
  participants: LiveQuizParticipantSnapshot[];
  leaderboard: LiveQuizLeaderboardEntry[];
}

export interface LiveQuizRealtimeMessage {
  roomId: string;
  examId: string;
  type: string;
  occurredAt: string;
  roomStatus: LiveQuizRoomStatus;
  studentTargetId: string | null;
  participant: LiveQuizParticipantSnapshot | null;
  leaderboard: LiveQuizLeaderboardEntry[] | null;
  summary: LiveQuizTeacherSummary | null;
}
