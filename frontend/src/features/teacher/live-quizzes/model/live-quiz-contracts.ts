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
  ownerTeacherId: string;
  status: LiveQuizRoomStatus;
  openedAt: string | null;
  startedAt: string | null;
  closedAt: string | null;
}
