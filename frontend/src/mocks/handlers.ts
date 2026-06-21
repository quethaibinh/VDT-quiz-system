import { http, HttpResponse } from "msw";
import type { PageResponse } from "@/lib/api-types";
import type {
  Assignment,
  ExamDetail,
  ExamDraftRequest,
  ExamSummary,
  StudentSummary,
} from "@/features/teacher/exams/model/exam-contracts";

const exams: ExamDetail[] = [];
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
    makeExam(subjectId, `${subjectId}-draft`, "Thi giữa kỳ", "DRAFT"),
    makeExam(subjectId, `${subjectId}-cancelled`, "Bài kiểm tra đã hủy", "CANCELLED"),
    makeExam(subjectId, `${subjectId}-closed`, "Bài kiểm tra đã hoàn thành", "CLOSED"),
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

  http.get("*/v1/api/exam-runtime-service/teacher/exams/:id/monitor", ({ params }) => HttpResponse.json({
    examId: params.id,
    participants: [
      { id: "s1", name: "Nguyễn Minh Anh", status: "ONLINE", answered: 32, total: 50, violations: 0, risk: "LOW", lastSeen: new Date().toISOString() },
      { id: "s2", name: "Trần Thu Hà", status: "OFFLINE", answered: 21, total: 50, violations: 2, risk: "HIGH", lastSeen: new Date(Date.now() - 40_000).toISOString() },
    ],
    events: [],
  })),
  http.get("*/v1/api/result-service/teacher/exams/:id/results", ({ params }) => HttpResponse.json({
    examId: params.id,
    participantCount: 120,
    gradedCount: 118,
    average: 7.2,
    highest: 9.8,
    lowest: 2.5,
    distribution: [],
    students: [],
  })),
];
