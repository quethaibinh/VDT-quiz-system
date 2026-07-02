import { http, HttpResponse } from "msw";
import type { PageResponse } from "@/lib/api-types";
import type {
  Assignment,
  ExamDetail,
  ExamDraftRequest,
  ExamSummary,
  StudentSummary,
} from "@/features/teacher/exams/model/exam-contracts";
import type { QuestionCollection } from "@/features/teacher/collections/model/collection-types";
import type {
  LiveQuizDetail,
  LiveQuizParticipantSnapshot,
  LiveQuizRequest,
  LiveQuizRoom,
  LiveQuizTeacherSnapshot,
  LiveQuizSummary,
} from "@/features/teacher/live-quizzes";
import type { Subject } from "@/features/teacher/subjects/model/subject-types";
import type { StudentAnswer, StudentPaperResponse, StudentQuestion } from "@/features/student/exams/model/student-exam-contracts";

const exams: ExamDetail[] = [];
const liveQuizzes: LiveQuizDetail[] = [];
const liveQuizRooms = new Map<string, LiveQuizRoom>();
const liveQuizParticipants = new Map<string, LiveQuizParticipantSnapshot[]>();
const liveQuizProgress = new Map<string, { index: number; score: number }>();
const collections: QuestionCollection[] = [
  {
    id: "collection-1",
    subjectId: "sub-1",
    ownerTeacherId: "teacher-1",
    name: "Bo cau hoi mau",
    description: "Du lieu mau cho live quiz.",
    visibility: "PRIVATE",
    status: "ACTIVE",
    editable: true,
    stats: { questionCount: 50, easy: 20, medium: 20, hard: 10 },
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  },
];
const subjects: Subject[] = [{
  id: "sub-1",
  code: "SUB001",
  name: "Mon hoc mau",
  description: "Du lieu mau cho giao vien.",
  status: "ACTIVE",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  assignedAt: "2026-01-01T00:00:00Z",
}];
type MockSession = {
  sessionId: string;
  examId: string;
  status: StudentPaperResponse["status"];
  serverTime: string;
  startAt: string;
  endAt: string;
  canStart: boolean;
  remainingSecondsToStart: number;
  serverStartedAt?: string;
  serverDeadlineAt?: string;
  questions: StudentQuestion[];
  answers: StudentAnswer[];
  serverSeq?: number;
};

const mockSessions = new Map<string, MockSession>();
const assignments = new Map<string, Assignment[]>();
const students: StudentSummary[] = Array.from({ length: 48 }, (_, index) => ({
  id: `student-${index + 1}`,
  studentCode: `SV${String(index + 1).padStart(4, "0")}`,
  fullName: `Học sinh ${index + 1}`,
  displayName: `Học sinh ${index + 1}`,
}));

function envelope<T>(data: T, status = 200, message = "Success") {
  return { timestamp: new Date().toISOString(), status, message, data };
}

function pageOf<T>(content: T[], page: number, size: number): PageResponse<T> {
  const totalPages = Math.ceil(content.length / size);
  return {
    content: content.slice(page * size, page * size + size),
    page,
    size,
    totalElements: content.length,
    totalPages,
    first: page === 0,
    last: page >= totalPages - 1,
  };
}

function getPageParams(request: Request, defaultSize = 20) {
  const searchParams = new URL(request.url).searchParams;
  return {
    searchParams,
    page: Number(searchParams.get("page") ?? 0),
    size: Number(searchParams.get("size") ?? defaultSize),
  };
}

function ensureSubjectExams(subjectId: string) {
  if (exams.some((exam) => exam.subjectId === subjectId)) return;
  exams.push(
    makeExam(subjectId, `${subjectId}-draft`, "Thi giữa kỳ (Bản nháp)", "DRAFT"),
    makeExam(subjectId, `${subjectId}-cancelled`, "Bài kiểm tra đã hủy", "CANCELLED"),
    makeExam(subjectId, `${subjectId}-closed`, "Bài kiểm tra đã hoàn thành", "CLOSED"),
    makeExam(subjectId, `${subjectId}-scheduled`, "Thi cuối kỳ (Sắp diễn ra)", "SCHEDULED", {
      startAt: new Date(Date.now() + 3600_000 * 24).toISOString(), // Ngay mai
      durationMinutes: 90,
    }),
    makeExam(subjectId, `${subjectId}-active`, "Thi giữa kỳ (Đang diễn ra)", "ACTIVE", {
      startAt: new Date(Date.now() - 600_000).toISOString(), // 10 phut truoc
      durationMinutes: 45,
    }),
  );
}

ensureSubjectExams("sub-1");

function ensureSubjectCollections(subjectId: string) {
  if (collections.some((collection) => collection.subjectId === subjectId)) return;
  collections.push({
    ...collections[0],
    id: `${subjectId}-collection-1`,
    subjectId,
  });
}

function ensureSubjectLiveQuizzes(subjectId: string) {
  if (liveQuizzes.some((quiz) => quiz.subjectId === subjectId)) return;
  ensureSubjectCollections(subjectId);
  liveQuizzes.push(
    makeLiveQuiz(subjectId, `${subjectId}-quiz-draft`, "Quiz on tap nhanh", "DRAFT"),
    makeLiveQuiz(subjectId, `${subjectId}-quiz-prepared`, "Quiz da chuan bi", "PREPARED"),
  );
}

function makeExam(
  subjectId: string,
  id: string,
  title: string,
  status: ExamDetail["status"],
  input?: Partial<ExamDraftRequest>,
): ExamDetail {
  const startAt = input?.startAt ?? "2026-07-01T08:00:00+07:00";
  const durationMinutes = input?.durationMinutes ?? 60;
  return {
    id,
    code: `EXAM-${id.slice(-6).toUpperCase()}`,
    title,
    description: input?.description ?? "",
    subjectId,
    subjectName: "Môn học đã chọn",
    collectionId: input?.collectionId ?? "collection-1",
    collectionName: "Bộ câu hỏi mẫu",
    easyCount: input?.easyCount ?? 20,
    mediumCount: input?.mediumCount ?? 20,
    hardCount: input?.hardCount ?? 10,
    startAt,
    endAt: new Date(new Date(startAt).getTime() + durationMinutes * 60_000).toISOString(),
    durationMinutes,
    joinBeforeMinutes: input?.joinBeforeMinutes ?? 10,
    joinAfterMinutes: input?.joinAfterMinutes ?? 0,
    shuffleQuestions: input?.shuffleQuestions ?? true,
    shuffleOptions: input?.shuffleOptions ?? true,
    showResultPolicy: input?.showResultPolicy ?? "AFTER_CLOSED",
    autoSubmit: input?.autoSubmit ?? true,
    requireFullscreen: input?.requireFullscreen ?? true,
    maxViolationAllowed: input?.maxViolationAllowed ?? 5,
    handleViolation: input?.handleViolation ?? "LOCK",
    assignedCount: assignments.get(id)?.length ?? 0,
    status,
    version: 0,
  };
}

function toSummary(exam: ExamDetail): ExamSummary {
  return {
    id: exam.id,
    code: exam.code,
    title: exam.title,
    subjectId: exam.subjectId,
    subjectName: exam.subjectName,
    collectionId: exam.collectionId,
    collectionName: exam.collectionName,
    startAt: exam.startAt,
    durationMinutes: exam.durationMinutes,
    questionCount: exam.easyCount + exam.mediumCount + exam.hardCount,
    assignedCount: assignments.get(exam.id)?.length ?? 0,
    status: exam.status,
    version: exam.version,
  };
}

function makeLiveQuiz(
  subjectId: string,
  id: string,
  title: string,
  status: LiveQuizDetail["status"],
  input?: Partial<LiveQuizRequest>,
): LiveQuizDetail {
  ensureSubjectCollections(subjectId);
  const collection = collections.find((item) => item.id === input?.collectionId)
    ?? collections.find((item) => item.subjectId === subjectId)
    ?? collections[0];
  return {
    id,
    code: `QUIZ-${id.slice(-6).toUpperCase()}`,
    title,
    description: input?.description ?? "",
    subjectId,
    subjectName: "Mon hoc da chon",
    collectionId: collection.id,
    collectionName: collection.name,
    questionCount: collection.stats.questionCount,
    shuffleQuestions: input?.shuffleQuestions ?? true,
    showLeaderboard: input?.showLeaderboard ?? true,
    showCorrectAnswer: input?.showCorrectAnswer ?? false,
    joinPolicy: "CODE_ONLY",
    status,
    snapshotVersion: status === "PREPARED" ? 1 : 0,
    preparedAt: status === "PREPARED" ? new Date().toISOString() : null,
    version: 0,
  };
}

function toLiveQuizSummary(quiz: LiveQuizDetail): LiveQuizSummary {
  return {
    id: quiz.id,
    code: quiz.code,
    title: quiz.title,
    subjectId: quiz.subjectId,
    subjectName: quiz.subjectName,
    collectionId: quiz.collectionId,
    collectionName: quiz.collectionName,
    questionCount: quiz.questionCount,
    shuffleQuestions: quiz.shuffleQuestions,
    showLeaderboard: quiz.showLeaderboard,
    showCorrectAnswer: quiz.showCorrectAnswer,
    joinPolicy: quiz.joinPolicy,
    status: quiz.status,
    snapshotVersion: quiz.snapshotVersion,
    version: quiz.version,
  };
}

function findLiveQuiz(subjectId: unknown, quizId: unknown) {
  return liveQuizzes.find(
    (quiz) => quiz.subjectId === String(subjectId) && quiz.id === String(quizId),
  );
}

function getOrCreateLiveQuizRoom(quiz: LiveQuizDetail): LiveQuizRoom {
  const existing = Array.from(liveQuizRooms.values()).find(
    (room) => room.examId === quiz.id && room.status !== "CLOSED",
  );
  if (existing) return existing;
  const room: LiveQuizRoom = {
    roomId: crypto.randomUUID(),
    examId: quiz.id,
    roomCode: "QZ" + String(liveQuizRooms.size + 1).padStart(4, "0"),
    quizTitle: quiz.title,
    subjectName: quiz.subjectName,
    questionCount: quiz.questionCount,
    showLeaderboard: quiz.showLeaderboard,
    ownerTeacherId: "teacher-1",
    status: "PREPARING",
    openedAt: null,
    startedAt: null,
    closedAt: null,
  };
  liveQuizRooms.set(room.roomId, room);
  return room;
}

function liveQuizSnapshot(room: LiveQuizRoom): LiveQuizTeacherSnapshot {
  const participants = liveQuizParticipants.get(room.roomId) ?? [];
  const leaderboard = participants
    .slice()
    .sort((a, b) => b.totalScore - a.totalScore || b.answeredCount - a.answeredCount)
    .map((participant, index) => ({
      rank: index + 1,
      participantId: participant.participantId,
      studentId: participant.studentId,
      studentName: participant.studentName,
      totalScore: participant.totalScore,
      answeredCount: participant.answeredCount,
      correctCount: participant.correctCount,
      timeoutCount: participant.timeoutCount,
      averageResponseMs: participant.averageResponseMs,
      finished: participant.status === "FINISHED",
    }));
  const ranked = participants.map((participant) => ({
    ...participant,
    currentRank: leaderboard.find((entry) => entry.participantId === participant.participantId)?.rank ?? null,
  }));
  return {
    roomId: room.roomId,
    examId: room.examId,
    roomCode: room.roomCode,
    quizTitle: room.quizTitle,
    subjectName: room.subjectName,
    questionCount: room.questionCount,
    showLeaderboard: room.showLeaderboard,
    roomStatus: room.status,
    serverTime: new Date().toISOString(),
    summary: {
      joined: ranked.filter((item) => item.status === "JOINED").length,
      inProgress: ranked.filter((item) => item.status === "IN_PROGRESS").length,
      finished: ranked.filter((item) => item.status === "FINISHED").length,
      disconnected: ranked.filter((item) => item.status === "DISCONNECTED").length,
    },
    participants: ranked,
    leaderboard,
  };
}

function mockCurrentQuestion(room: LiveQuizRoom, progress: { index: number; score: number }) {
  const position = progress.index + 1;
  const multipleChoice = position % 3 === 0;
  const startedAt = new Date().toISOString();
  return {
    participantId: "mock-participant",
    questionId: `live-${room.roomId}-q-${position}`,
    questionPosition: position,
    totalQuestions: Math.min(room.questionCount, 10),
    answeredCount: progress.index,
    totalScore: progress.score,
    maxScore: Math.min(room.questionCount, 10),
    currentRank: 1,
    type: multipleChoice ? "MULTI_CHOICE" : "SINGLE_CHOICE",
    content: `Cau hoi live quiz so ${position}: ${multipleChoice ? "Hay chon cac dap an dung." : "Hay chon dap an dung nhat."}`,
    contentFormat: "TEXT",
    options: ["A", "B", "C", "D"].map((key) => ({
      optionId: `live-${room.roomId}-q-${position}-${key}`,
      key,
      content: `Lua chon ${key}`,
      contentFormat: "TEXT",
    })),
    startedAt,
    endsAt: new Date(Date.now() + 30_000).toISOString(),
    serverTime: startedAt,
  };
}

function findExam(subjectId: unknown, examId: unknown) {
  return exams.find(
    (exam) => exam.subjectId === String(subjectId) && exam.id === String(examId),
  );
}

function createAssignment(studentId: string): Assignment {
  const student = students.find((item) => item.id === studentId);
  return {
    id: crypto.randomUUID(),
    studentId,
    studentCode: student?.studentCode ?? studentId,
    studentName: student?.fullName ?? studentId,
    status: "ASSIGNED",
    assignedAt: new Date().toISOString(),
    removedAt: null,
  };
}

export const handlers = [
  http.get("*/v1/api/question-service/teacher/subjects", ({ request }) => {
    const searchParams = new URL(request.url).searchParams;
    const keyword = (searchParams.get("keyword") ?? "").toLowerCase();
    const status = searchParams.get("status");
    const filtered = subjects
      .filter((subject) => !status || subject.status === status)
      .filter((subject) => !keyword || `${subject.code} ${subject.name}`.toLowerCase().includes(keyword));
    return HttpResponse.json(envelope(filtered));
  }),

  http.get("*/v1/api/question-service/teacher/subjects/:subjectId", ({ params }) => {
    const subject = subjects.find((item) => item.id === String(params.subjectId));
    return subject
      ? HttpResponse.json(envelope(subject))
      : HttpResponse.json({ message: "Khong tim thay mon hoc." }, { status: 404 });
  }),

  http.get("*/v1/api/auth-service/teacher/students", ({ request }) => {
    const { searchParams, page, size } = getPageParams(request);
    const keyword = (searchParams.get("keyword") ?? "").toLowerCase();
    const filtered = students.filter((student) =>
      `${student.studentCode} ${student.fullName}`.toLowerCase().includes(keyword),
    );
    return HttpResponse.json(envelope(pageOf(filtered, page, size)));
  }),

  http.get("*/v1/api/question-service/teacher/subjects/:subjectId/question-collections", ({ params, request }) => {
    const subjectId = String(params.subjectId);
    ensureSubjectCollections(subjectId);
    const { searchParams, page, size } = getPageParams(request);
    const status = searchParams.get("status");
    const keyword = (searchParams.get("keyword") ?? "").toLowerCase();
    const filtered = collections
      .filter((collection) => collection.subjectId === subjectId)
      .filter((collection) => !status || collection.status === status)
      .filter((collection) => !keyword || collection.name.toLowerCase().includes(keyword));
    return HttpResponse.json(envelope(pageOf(filtered, page, size)));
  }),

  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/live-quizzes", ({ params, request }) => {
    const subjectId = String(params.subjectId);
    ensureSubjectLiveQuizzes(subjectId);
    const { searchParams, page, size } = getPageParams(request);
    const status = searchParams.get("status");
    const keyword = (searchParams.get("keyword") ?? "").toLowerCase();
    const filtered = liveQuizzes
      .filter((quiz) => quiz.subjectId === subjectId)
      .filter((quiz) => !status || quiz.status === status)
      .filter((quiz) => !keyword || `${quiz.code} ${quiz.title}`.toLowerCase().includes(keyword))
      .map(toLiveQuizSummary);
    return HttpResponse.json(envelope(pageOf(filtered, page, size)));
  }),

  http.post("*/v1/api/exam-service/teacher/subjects/:subjectId/live-quizzes", async ({ params, request }) => {
    const input = await request.json() as LiveQuizRequest;
    const quiz = makeLiveQuiz(String(params.subjectId), crypto.randomUUID(), input.title, "DRAFT", input);
    liveQuizzes.unshift(quiz);
    return HttpResponse.json(envelope(quiz, 201, "Created"), { status: 201 });
  }),

  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/live-quizzes/:quizId", ({ params }) => {
    const quiz = findLiveQuiz(params.subjectId, params.quizId);
    return quiz
      ? HttpResponse.json(envelope(quiz))
      : HttpResponse.json({ message: "Khong tim thay quiz." }, { status: 404 });
  }),

  http.put("*/v1/api/exam-service/teacher/subjects/:subjectId/live-quizzes/:quizId", async ({ params, request }) => {
    const index = liveQuizzes.findIndex((quiz) => quiz.id === params.quizId && quiz.subjectId === params.subjectId);
    if (index < 0) return HttpResponse.json({ message: "Khong tim thay quiz." }, { status: 404 });
    if (liveQuizzes[index].status !== "DRAFT") {
      return HttpResponse.json({ message: "Quiz khong con co the chinh sua." }, { status: 409 });
    }
    const input = await request.json() as LiveQuizRequest;
    liveQuizzes[index] = {
      ...makeLiveQuiz(String(params.subjectId), String(params.quizId), input.title, "DRAFT", input),
      version: liveQuizzes[index].version + 1,
    };
    return HttpResponse.json(envelope(liveQuizzes[index]));
  }),

  http.post("*/v1/api/exam-service/teacher/subjects/:subjectId/live-quizzes/:quizId/prepare", ({ params }) => {
    const quiz = findLiveQuiz(params.subjectId, params.quizId);
    if (!quiz) return HttpResponse.json({ message: "Khong tim thay quiz." }, { status: 404 });
    quiz.status = "PREPARED";
    quiz.snapshotVersion = 1;
    quiz.preparedAt = quiz.preparedAt ?? new Date().toISOString();
    quiz.version += 1;
    const room = getOrCreateLiveQuizRoom(quiz);
    return HttpResponse.json(envelope({
      quiz,
      roomId: room.roomId,
      roomCode: room.roomCode,
      roomStatus: room.status,
    }));
  }),

  http.get("*/v1/api/examruntime-service/teacher/live-quizzes/:roomId", ({ params }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    return room
      ? HttpResponse.json(envelope(room))
      : HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
  }),

  http.post("*/v1/api/examruntime-service/teacher/live-quizzes/:roomId/open", ({ params }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    if (!room) return HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
    if (room.status === "CLOSED") return HttpResponse.json({ message: "Phong quiz da dong." }, { status: 409 });
    room.status = "OPEN";
    room.openedAt = room.openedAt ?? new Date().toISOString();
    return HttpResponse.json(envelope(room));
  }),

  http.post("*/v1/api/examruntime-service/teacher/live-quizzes/:roomId/start", ({ params }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    if (!room) return HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
    if (room.status !== "OPEN" && room.status !== "STARTED") return HttpResponse.json({ message: "Phong quiz chua mo lobby." }, { status: 409 });
    room.status = "STARTED";
    room.startedAt = room.startedAt ?? new Date().toISOString();
    const participants = liveQuizParticipants.get(room.roomId) ?? [];
    liveQuizParticipants.set(room.roomId, participants.map((item) => item.status === "JOINED" ? { ...item, status: "IN_PROGRESS", startedAt: item.startedAt ?? new Date().toISOString() } : item));
    return HttpResponse.json(envelope(room));
  }),

  http.get("*/v1/api/examruntime-service/teacher/live-quizzes/:roomId/snapshot", ({ params }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    return room
      ? HttpResponse.json(envelope(liveQuizSnapshot(room)))
      : HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
  }),

  http.post("*/v1/api/examruntime-service/teacher/live-quizzes/:roomId/close", ({ params }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    if (!room) return HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
    room.status = "CLOSED";
    room.closedAt = room.closedAt ?? new Date().toISOString();
    return HttpResponse.json(envelope(room));
  }),

  http.post("*/v1/api/examruntime-service/student/live-quizzes/join", async ({ request }) => {
    const body = await request.json() as { code: string };
    const room = Array.from(liveQuizRooms.values()).find((item) => item.roomCode === body.code?.trim().toUpperCase());
    if (!room) return HttpResponse.json({ message: "Ma phong khong dung." }, { status: 404 });
    const existing = (liveQuizParticipants.get(room.roomId) ?? []).find((item) => item.studentId === "student-mock");
    if (!existing && room.status !== "OPEN") return HttpResponse.json({ message: "Phong chua cho vao hoac da khoa." }, { status: 409 });
    const participants = liveQuizParticipants.get(room.roomId) ?? [];
    const participant = existing ?? {
      participantId: "mock-participant",
      studentId: "student-mock",
      studentCode: "SV0001",
      studentName: "Hoc sinh demo",
      status: "JOINED" as const,
      answeredCount: 0,
      totalQuestions: Math.min(room.questionCount, 10),
      correctCount: 0,
      wrongCount: 0,
      timeoutCount: 0,
      totalScore: 0,
      maxScore: Math.min(room.questionCount, 10),
      averageResponseMs: null,
      currentRank: null,
      joinedAt: new Date().toISOString(),
      startedAt: null,
      finishedAt: null,
      lastSeenAt: new Date().toISOString(),
    };
    if (!existing) liveQuizParticipants.set(room.roomId, [...participants, participant]);
    liveQuizProgress.set(room.roomId, liveQuizProgress.get(room.roomId) ?? { index: participant.answeredCount, score: participant.totalScore });
    return HttpResponse.json(envelope({
      roomId: room.roomId,
      examId: room.examId,
      participantId: participant.participantId,
      roomCode: room.roomCode,
      quizTitle: room.quizTitle,
      subjectName: room.subjectName,
      status: participant.status,
      roomStatus: room.status,
      totalQuestions: participant.totalQuestions,
      participantCount: liveQuizParticipants.get(room.roomId)?.length ?? 1,
      currentRank: participant.currentRank,
      serverTime: new Date().toISOString(),
    }));
  }),

  http.get("*/v1/api/examruntime-service/student/live-quizzes/:roomId/state", ({ params }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    if (!room) return HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
    const progress = liveQuizProgress.get(room.roomId) ?? { index: 0, score: 0 };
    const total = Math.min(room.questionCount, 10);
    return HttpResponse.json(envelope({
      roomId: room.roomId,
      examId: room.examId,
      participantId: "mock-participant",
      roomCode: room.roomCode,
      quizTitle: room.quizTitle,
      subjectName: room.subjectName,
      roomStatus: room.status,
      participantStatus: progress.index >= total ? "FINISHED" : room.status === "STARTED" ? "IN_PROGRESS" : "JOINED",
      answeredCount: progress.index,
      totalQuestions: total,
      totalScore: progress.score,
      maxScore: total,
      currentRank: 1,
      participantCount: liveQuizParticipants.get(room.roomId)?.length ?? 1,
      serverTime: new Date().toISOString(),
      currentQuestionEndsAt: room.status === "STARTED" ? new Date(Date.now() + 30_000).toISOString() : null,
    }));
  }),

  http.get("*/v1/api/examruntime-service/student/live-quizzes/:roomId/current-question", ({ params }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    if (!room) return HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
    if (room.status !== "STARTED") return HttpResponse.json({ message: "Quiz chua bat dau." }, { status: 409 });
    const progress = liveQuizProgress.get(room.roomId) ?? { index: 0, score: 0 };
    if (progress.index >= Math.min(room.questionCount, 10)) return HttpResponse.json({ message: "Da hoan thanh." }, { status: 409 });
    return HttpResponse.json(envelope(mockCurrentQuestion(room, progress)));
  }),

  http.post("*/v1/api/examruntime-service/student/live-quizzes/:roomId/answer", async ({ params, request }) => {
    const room = liveQuizRooms.get(String(params.roomId));
    if (!room) return HttpResponse.json({ message: "Khong tim thay phong quiz." }, { status: 404 });
    const body = await request.json() as { questionId: string; selectedOptionIds: string[] };
    const progress = liveQuizProgress.get(room.roomId) ?? { index: 0, score: 0 };
    const position = progress.index + 1;
    const multipleChoice = position % 3 === 0;
    const selectedKeys = body.selectedOptionIds.map((optionId) => optionId.slice(-1)).sort();
    const correct = multipleChoice
      ? selectedKeys.length === 2 && selectedKeys[0] === "A" && selectedKeys[1] === "B"
      : selectedKeys.length === 1 && selectedKeys[0] === "A";
    const total = Math.min(room.questionCount, 10);
    const scoreAwarded = correct ? 0.75 : 0;
    const next = { index: progress.index + 1, score: progress.score + scoreAwarded };
    liveQuizProgress.set(room.roomId, next);
    const participants = liveQuizParticipants.get(room.roomId) ?? [];
    liveQuizParticipants.set(room.roomId, participants.map((item) => item.participantId === "mock-participant"
      ? {
          ...item,
          status: next.index >= total ? "FINISHED" : "IN_PROGRESS",
          answeredCount: next.index,
          correctCount: item.correctCount + (correct ? 1 : 0),
          wrongCount: item.wrongCount + (correct ? 0 : 1),
          totalScore: next.score,
          currentRank: 1,
          finishedAt: next.index >= total ? new Date().toISOString() : null,
        }
      : item));
    return HttpResponse.json(envelope({
      questionId: body.questionId,
      questionPosition: position,
      answerStatus: "ANSWERED",
      correct,
      scoreAwarded,
      maxScore: 1,
      responseTimeMs: 5000,
      scoreRatio: scoreAwarded,
      totalScore: next.score,
      nextQuestionAvailable: next.index < total,
      finished: next.index >= total,
    }));
  }),

  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/exams", ({ params, request }) => {
    const subjectId = String(params.subjectId);
    ensureSubjectExams(subjectId);
    const { searchParams, page, size } = getPageParams(request);
    const status = searchParams.get("status");
    const keyword = (searchParams.get("keyword") ?? "").toLowerCase();
    const filtered = exams
      .filter((exam) => exam.subjectId === subjectId)
      .filter((exam) => !status || exam.status === status)
      .filter((exam) => !keyword || `${exam.code} ${exam.title}`.toLowerCase().includes(keyword))
      .map(toSummary);
    return HttpResponse.json(envelope(pageOf(filtered, page, size)));
  }),

  http.post("*/v1/api/exam-service/teacher/subjects/:subjectId/exams", async ({ params, request }) => {
    const input = await request.json() as ExamDraftRequest;
    const exam = makeExam(String(params.subjectId), crypto.randomUUID(), input.title, "DRAFT", input);
    exams.unshift(exam);
    return HttpResponse.json(envelope(exam, 201, "Created"), { status: 201 });
  }),

  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId", ({ params }) => {
    const exam = findExam(params.subjectId, params.examId);
    return exam
      ? HttpResponse.json(envelope(exam))
      : HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
  }),

  http.put("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId", async ({ params, request }) => {
    const index = exams.findIndex((item) => item.id === params.examId && item.subjectId === params.subjectId);
    if (index < 0) return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    if (exams[index].status !== "DRAFT") {
      return HttpResponse.json({ message: "Ca thi không còn có thể chỉnh sửa." }, { status: 409 });
    }
    const input = await request.json() as ExamDraftRequest;
    exams[index] = { ...makeExam(String(params.subjectId), String(params.examId), input.title, "DRAFT", input), version: exams[index].version + 1 };
    return HttpResponse.json(envelope(exams[index]));
  }),

  http.patch("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/cancel", ({ params }) => {
    const exam = findExam(params.subjectId, params.examId);
    if (!exam) return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    if (exam.status !== "DRAFT") {
      return HttpResponse.json({ message: "Ca thi không còn có thể hủy." }, { status: 409 });
    }
    exam.status = "CANCELLED";
    exam.version += 1;
    return HttpResponse.json(envelope(exam));
  }),

  http.patch("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/schedule", ({ params }) => {
    const exam = findExam(params.subjectId, params.examId);
    if (!exam) {
      return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    }
    if (exam.status === "SCHEDULED") {
      return HttpResponse.json(envelope(exam));
    }
    if (exam.status !== "DRAFT") {
      return HttpResponse.json({ message: "Ca thi không còn ở trạng thái bản nháp." }, { status: 409 });
    }
    const assignedCount = assignments.get(exam.id)?.length ?? exam.assignedCount;
    if (assignedCount < 1) {
      return HttpResponse.json({ message: "Cần phân công ít nhất 1 học sinh." }, { status: 409 });
    }
    if (new Date(exam.startAt).getTime() <= Date.now()) {
      return HttpResponse.json({ message: "Thời gian bắt đầu phải ở tương lai." }, { status: 409 });
    }
    exam.status = "SCHEDULED";
    exam.assignedCount = assignedCount;
    exam.version += 1;
    return HttpResponse.json(envelope(exam));
  }),

  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/assignments", ({ params, request }) => {
    const { page, size } = getPageParams(request);
    return HttpResponse.json(envelope(pageOf(assignments.get(String(params.examId)) ?? [], page, size)));
  }),

  http.post("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/assignments", async ({ params, request }) => {
    const exam = findExam(params.subjectId, params.examId);
    if (!exam) return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    if (exam.status !== "DRAFT") {
      return HttpResponse.json({ message: "Không thể thay đổi học sinh của ca thi này." }, { status: 409 });
    }
    const { studentIds } = await request.json() as { studentIds: string[] };
    const current = assignments.get(String(params.examId)) ?? [];
    const existing = new Set(current.map((assignment) => assignment.studentId));
    const added = studentIds
      .filter((id) => !existing.has(id))
      .map(createAssignment);
    assignments.set(String(params.examId), [...current, ...added]);
    exam.assignedCount = current.length + added.length;
    return HttpResponse.json(envelope({
      requestedCount: studentIds.length,
      assignedCount: added.length,
      unchangedCount: studentIds.length - added.length,
      assignments: added,
    }));
  }),

  http.delete("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/assignments/:studentId", ({ params }) => {
    const exam = findExam(params.subjectId, params.examId);
    if (!exam) return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    if (exam.status !== "DRAFT") {
      return HttpResponse.json({ message: "Không thể thay đổi học sinh của ca thi này." }, { status: 409 });
    }
    const current = assignments.get(String(params.examId)) ?? [];
    const removed = current.find((item) => item.studentId === params.studentId);
    assignments.set(String(params.examId), current.filter((item) => item.studentId !== params.studentId));
    exam.assignedCount = Math.max(0, current.length - (removed ? 1 : 0));
    return HttpResponse.json(envelope({
      requestedCount: 1,
      assignedCount: removed ? 1 : 0,
      unchangedCount: removed ? 0 : 1,
      assignments: removed ? [removed] : [],
    }));
  }),

  http.get("*/v1/api/examruntime-service/teacher/exams/:id/monitor", ({ params }) => HttpResponse.json({
    examId: params.id,
    serverTime: new Date().toISOString(),
    participants: [
      {
        sessionId: "session-1",
        studentId: "student-1",
        studentCode: "SV0001",
        studentName: "Nguyen Minh Anh",
        status: "ONLINE",
        answeredCount: 32,
        totalQuestions: 50,
        totalViolationCount: 0,
        riskScore: 0,
        riskLevel: "LOW",
        locked: false,
        lastSeenAt: new Date().toISOString(),
        lastHeartbeatAt: new Date().toISOString(),
        lastEventAt: null,
      },
      {
        sessionId: "session-2",
        studentId: "student-2",
        studentCode: "SV0002",
        studentName: "Tran Thu Ha",
        status: "OFFLINE",
        answeredCount: 21,
        totalQuestions: 50,
        totalViolationCount: 2,
        riskScore: 8,
        riskLevel: "HIGH",
        locked: false,
        lastSeenAt: new Date(Date.now() - 40_000).toISOString(),
        lastHeartbeatAt: new Date(Date.now() - 40_000).toISOString(),
        lastEventAt: new Date(Date.now() - 45_000).toISOString(),
      },
      {
        sessionId: null,
        studentId: "student-3",
        studentCode: "SV0003",
        studentName: "Le Hoang Nam",
        status: "NOT_JOIN",
        answeredCount: 0,
        totalQuestions: 0,
        totalViolationCount: 0,
        riskScore: 0,
        riskLevel: "LOW",
        locked: false,
        lastSeenAt: null,
        lastHeartbeatAt: null,
        lastEventAt: null,
      },
    ],
    events: [{
      id: "event-1",
      examId: String(params.id),
      sessionId: "session-2",
      studentId: "student-2",
      eventType: "FULLSCREEN_EXIT",
      severity: "HIGH",
      occurredAt: new Date(Date.now() - 45_000).toISOString(),
      receivedAt: new Date(Date.now() - 44_000).toISOString(),
      metadata: "{}",
      countInSession: 2,
    }],
  })),
  // Lay danh sach ca thi cua hoc sinh
  http.get("*/v1/api/exam-service/student/exams", ({ request }) => {
    const { searchParams, page, size } = getPageParams(request);
    const statusFilter = searchParams.get("status");
    const now = new Date();

    const studentExams = exams
      .filter((exam) => exam.status !== "DRAFT" && exam.status !== "CANCELLED")
      .map((exam) => {
        let availability: "UPCOMING" | "OPEN" | "ENDED" = "OPEN";
        const start = new Date(exam.startAt);
        const end = new Date(exam.endAt);
        if (now < start) {
          availability = "UPCOMING";
        } else if (now > end || exam.status === "CLOSED") {
          availability = "ENDED";
        }

        return {
          examId: exam.id,
          code: exam.code,
          title: exam.title,
          subjectId: exam.subjectId,
          subjectName: exam.subjectName,
          startAt: exam.startAt,
          endAt: exam.endAt,
          durationMinutes: exam.durationMinutes,
          questionCount: exam.easyCount + exam.mediumCount + exam.hardCount,
          status: exam.status,
          studentAvailability: availability,
          assignmentStatus: "ASSIGNED",
        };
      })
      .filter((exam) => !statusFilter || exam.studentAvailability === statusFilter);

    return HttpResponse.json(envelope({
      serverTime: new Date().toISOString(),
      ...pageOf(studentExams, page, size),
    }));
  }),

  // Lay chi tiet ca thi cua hoc sinh
  http.get("*/v1/api/exam-service/student/exams/:examId", ({ params }) => {
    const exam = exams.find((item) => item.id === params.examId);
    if (!exam || exam.status === "DRAFT" || exam.status === "CANCELLED") {
      return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    }
    const now = new Date();
    let availability: "UPCOMING" | "OPEN" | "ENDED" = "OPEN";
    const start = new Date(exam.startAt);
    const end = new Date(exam.endAt);
    if (now < start) {
      availability = "UPCOMING";
    } else if (now > end || exam.status === "CLOSED") {
      availability = "ENDED";
    }

    return HttpResponse.json(envelope({
      examId: exam.id,
      code: exam.code,
      title: exam.title,
      description: exam.description,
      subjectId: exam.subjectId,
      subjectName: exam.subjectName,
      startAt: exam.startAt,
      endAt: exam.endAt,
      durationMinutes: exam.durationMinutes,
      questionCount: exam.easyCount + exam.mediumCount + exam.hardCount,
      status: exam.status,
      studentAvailability: availability,
      assignmentStatus: "ASSIGNED",
    }));
  }),

  // Join vao ca thi de nhan session
  http.post("*/v1/api/examruntime-service/student/exams/:examId/join", ({ params }) => {
    const examId = String(params.examId);
    const exam = exams.find((item) => item.id === examId);
    if (!exam) {
      return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    }

    let session = Array.from(mockSessions.values()).find((s) => s.examId === examId);
    if (!session) {
      const sessionId = crypto.randomUUID();
      session = {
        sessionId,
        examId,
        status: "CREATED",
        serverTime: new Date().toISOString(),
        startAt: exam.startAt,
        endAt: exam.endAt,
        canStart: new Date() >= new Date(exam.startAt),
        remainingSecondsToStart: Math.max(0, Math.ceil((new Date(exam.startAt).getTime() - Date.now()) / 1000)),
        questions: [],
        answers: [],
      };
      mockSessions.set(sessionId, session);
    } else {
      session.serverTime = new Date().toISOString();
      session.canStart = new Date() >= new Date(session.startAt);
      session.remainingSecondsToStart = Math.max(0, Math.ceil((new Date(session.startAt).getTime() - Date.now()) / 1000));
    }

    return HttpResponse.json(envelope({
      sessionId: session.sessionId,
      examId: session.examId,
      status: session.status,
      serverTime: session.serverTime,
      startAt: session.startAt,
      endAt: session.endAt,
      canStart: session.canStart,
      remainingSecondsToStart: session.remainingSecondsToStart,
    }));
  }),

  // Bat dau lam bai thi
  http.post("*/v1/api/examruntime-service/student/exams/:examId/start", ({ params }) => {
    const examId = String(params.examId);
    const exam = exams.find((item) => item.id === examId);
    if (!exam) {
      return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    }

    const session = Array.from(mockSessions.values()).find((s) => s.examId === examId);
    if (!session) {
      return HttpResponse.json({ message: "Session chưa được khởi tạo." }, { status: 400 });
    }

    if (session.status === "CREATED") {
      session.status = "IN_PROGRESS";
      session.serverStartedAt = new Date().toISOString();
      session.serverDeadlineAt = exam.endAt;

      // Sinh 10 cau hoi gia dinh tu collection
      session.questions = Array.from({ length: 10 }, (_, i): StudentQuestion => ({
        questionId: `q-${examId}-${i + 1}`,
        difficulty: i < 4 ? "EASY" : i < 8 ? "MEDIUM" : "HARD",
        type: i % 3 === 0 ? "MULTIPLE_CHOICE" : "SINGLE_CHOICE",
        content: `Nội dung câu hỏi trắc nghiệm số ${i + 1} của ca thi ${exam.title}.`,
        contentFormat: "TEXT",
        score: 1.0,
        options: [
          { optionId: `opt-${examId}-${i + 1}-a`, key: "A", content: `Đáp án lựa chọn A cho câu ${i + 1}`, contentFormat: "TEXT" },
          { optionId: `opt-${examId}-${i + 1}-b`, key: "B", content: `Đáp án lựa chọn B cho câu ${i + 1}`, contentFormat: "TEXT" },
          { optionId: `opt-${examId}-${i + 1}-c`, key: "C", content: `Đáp án lựa chọn C cho câu ${i + 1}`, contentFormat: "TEXT" },
          { optionId: `opt-${examId}-${i + 1}-d`, key: "D", content: `Đáp án lựa chọn D cho câu ${i + 1}`, contentFormat: "TEXT" },
        ],
      }));
      session.answers = [];
    }

    return HttpResponse.json(envelope({
      sessionId: session.sessionId,
      status: session.status,
      serverStartedAt: session.serverStartedAt,
      serverDeadlineAt: session.serverDeadlineAt,
      questions: session.questions,
      answers: session.answers,
    }));
  }),

  // Lay lai session da co
  http.get("*/v1/api/examruntime-service/student/sessions/:sessionId", ({ params }) => {
    const sessionId = String(params.sessionId);
    const session = mockSessions.get(sessionId);
    if (!session) {
      return HttpResponse.json({ message: "Không tìm thấy phiên thi." }, { status: 404 });
    }

    return HttpResponse.json(envelope({
      sessionId: session.sessionId,
      status: session.status,
      serverStartedAt: session.serverStartedAt,
      serverDeadlineAt: session.serverDeadlineAt,
      questions: session.questions,
      answers: session.answers,
    }));
  }),

  // Luu dap an tu dong
  http.put("*/v1/api/examruntime-service/student/sessions/:sessionId/answers", async ({ params, request }) => {
    const sessionId = String(params.sessionId);
    const session = mockSessions.get(sessionId);
    if (!session) {
      return HttpResponse.json({ message: "Không tìm thấy phiên thi." }, { status: 404 });
    }

    const body = await request.json() as { clientSeq: number; answers: StudentAnswer[] };
    
    // Merge batch autosave vao cac dap an da co
    const byQuestion = new Map(session.answers.map((answer) => [answer.questionId, answer]));
    body.answers.forEach((answer) => byQuestion.set(answer.questionId, answer));
    session.answers = Array.from(byQuestion.values());

    return HttpResponse.json(envelope({
      sessionId: session.sessionId,
      acceptedSeq: body.clientSeq,
      serverSeq: (session.serverSeq ?? 0) + 1,
      savedCount: body.answers.length,
      skippedCount: 0,
      storeMode: "REDIS",
      lastAutosaveAt: new Date().toISOString(),
    }));
  }),
];
