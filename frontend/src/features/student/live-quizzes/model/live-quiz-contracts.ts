export type StudentLiveQuizRoomStatus = "PREPARING" | "OPEN" | "STARTED" | "CLOSED";
export type StudentLiveQuizParticipantStatus = "JOINED" | "IN_PROGRESS" | "FINISHED" | "DISCONNECTED";
export type LiveQuizAnswerStatus = "ANSWERED" | "TIMEOUT";
export type StudentLiveQuizQuestionType = "SINGLE_CHOICE" | "MULTI_CHOICE" | "MULTIPLE_CHOICE" | string;

export interface StudentLiveQuizJoinRequest {
  code: string;
}

export interface StudentLiveQuizJoinResponse {
  roomId: string;
  examId: string;
  participantId: string;
  roomCode: string;
  quizTitle: string;
  subjectName: string;
  status: StudentLiveQuizParticipantStatus;
  roomStatus: StudentLiveQuizRoomStatus;
  totalQuestions: number;
  participantCount: number;
  currentRank: number | null;
  serverTime: string;
}

export interface StudentLiveQuizState {
  roomId: string;
  examId: string;
  participantId: string;
  roomCode: string;
  quizTitle: string;
  subjectName: string;
  roomStatus: StudentLiveQuizRoomStatus;
  participantStatus: StudentLiveQuizParticipantStatus;
  answeredCount: number;
  totalQuestions: number;
  totalScore: number;
  maxScore: number;
  currentRank: number | null;
  participantCount: number;
  serverTime: string;
  currentQuestionEndsAt: string | null;
}

export interface StudentLiveQuizOption {
  optionId: string;
  key: string;
  content: string;
  contentFormat: string | null;
}

export interface StudentLiveQuizCurrentQuestion {
  participantId: string;
  questionId: string;
  questionPosition: number;
  totalQuestions: number;
  answeredCount: number;
  totalScore: number;
  maxScore: number;
  currentRank: number | null;
  type: StudentLiveQuizQuestionType;
  content: string;
  contentFormat: string | null;
  options: StudentLiveQuizOption[];
  startedAt: string;
  endsAt: string;
  serverTime: string;
}

export interface StudentLiveQuizAnswerRequest {
  questionId: string;
  selectedOptionIds: string[];
}

export interface StudentLiveQuizAnswerResponse {
  questionId: string;
  questionPosition: number;
  answerStatus: LiveQuizAnswerStatus;
  correct: boolean;
  scoreAwarded: number;
  maxScore?: number;
  responseTimeMs?: number | null;
  scoreRatio?: number | null;
  totalScore: number;
  nextQuestionAvailable: boolean;
  finished: boolean;
}
