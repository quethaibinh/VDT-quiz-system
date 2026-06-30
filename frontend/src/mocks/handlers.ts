import { http, HttpResponse } from "msw";
import type { PageResponse } from "@/lib/api-types";
import type {
  Assignment,
  ExamDetail,
  ExamDraftRequest,
  ExamSummary,
  StudentSummary,
} from "@/features/teacher/exams/model/exam-contracts";
import type { Subject } from "@/features/teacher/subjects/model/subject-types";
import type { StudentAnswer, StudentPaperResponse, StudentQuestion } from "@/features/student/exams/model/student-exam-contracts";

const exams: ExamDetail[] = [];
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
