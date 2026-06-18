import { http, HttpResponse } from "msw";
import type { ExamSummary } from "@/features/teacher/exams/model/exam-contracts";

const exams: ExamSummary[] = [];

function ensureSubjectExams(subjectId: string) {
  if (exams.some((exam) => exam.subjectId === subjectId)) return;
  exams.push(
    { id: `${subjectId}-draft`, title: "Thi giữa kỳ", subjectId, subjectName: "Môn học đã chọn", startAt: "2026-06-14T08:00:00+07:00", durationMinutes: 60, questionCount: 50, assignedCount: 120, status: "DRAFT" },
    { id: `${subjectId}-active`, title: "Kiểm tra chương 1-3", subjectId, subjectName: "Môn học đã chọn", startAt: "2026-06-12T14:00:00+07:00", durationMinutes: 45, questionCount: 40, assignedCount: 80, status: "ACTIVE" },
    { id: `${subjectId}-closed`, title: "Bài kiểm tra đã hoàn thành", subjectId, subjectName: "Môn học đã chọn", startAt: "2026-06-05T09:00:00+07:00", durationMinutes: 45, questionCount: 40, assignedCount: 76, status: "CLOSED" },
  );
}

export const handlers = [
  http.get("*/v1/api/exam-service/teacher/exams", ({ request }) => {
    const url = new URL(request.url);
    const subjectId = url.searchParams.get("subjectId");
    const status = url.searchParams.get("status");
    if (!subjectId) return HttpResponse.json({ message: "subjectId là bắt buộc." }, { status: 400 });
    ensureSubjectExams(subjectId);
    return HttpResponse.json(exams.filter((exam) => exam.subjectId === subjectId && (!status || exam.status === status)));
  }),
  http.post("*/v1/api/exam-service/teacher/exams", async ({ request }) => {
    const input = await request.json() as Record<string, unknown>;
    const exam: ExamSummary = {
      id: crypto.randomUUID(),
      title: String(input.title),
      subjectId: String(input.subjectId),
      subjectName: "Môn học đã chọn",
      startAt: String(input.startAt),
      durationMinutes: Number(input.durationMinutes),
      questionCount: Number(input.easyCount) + Number(input.mediumCount) + Number(input.hardCount),
      assignedCount: 120,
      status: "DRAFT",
    };
    exams.unshift(exam);
    return HttpResponse.json(exam, { status: 201 });
  }),
  http.post("*/v1/api/exam-service/teacher/exams/:id/activate", ({ params }) => {
    const exam = exams.find((item) => item.id === params.id);
    if (!exam) return HttpResponse.json({ message: "Không tìm thấy ca thi." }, { status: 404 });
    exam.status = "SCHEDULED";
    return HttpResponse.json(exam);
  }),
  http.get("*/v1/api/exam-runtime-service/teacher/exams/:id/monitor", ({ params }) => HttpResponse.json({
    examId: params.id,
    participants: [
      { id: "s1", name: "Nguyễn Minh Anh", status: "ONLINE", answered: 32, total: 50, violations: 0, risk: "LOW", lastSeen: new Date().toISOString() },
      { id: "s2", name: "Trần Thu Hà", status: "OFFLINE", answered: 21, total: 50, violations: 2, risk: "HIGH", lastSeen: new Date(Date.now() - 40_000).toISOString() },
      { id: "s3", name: "Lê Hoàng Nam", status: "SUBMITTED", answered: 50, total: 50, violations: 1, risk: "LOW", lastSeen: new Date().toISOString() },
    ],
    events: [
      { id: "e1", studentName: "Trần Thu Hà", type: "Thoát chế độ toàn màn hình lần 2", occurredAt: new Date().toISOString() },
      { id: "e2", studentName: "Lê Hoàng Nam", type: "Đã nộp bài", occurredAt: new Date(Date.now() - 60_000).toISOString() },
    ],
  })),
  http.get("*/v1/api/result-service/teacher/exams/:id/results", ({ params }) => HttpResponse.json({
    examId: params.id, participantCount: 120, gradedCount: 118, average: 7.2, highest: 9.8, lowest: 2.5,
    distribution: [{ range: "0-2", count: 2 }, { range: "2-4", count: 8 }, { range: "4-6", count: 22 }, { range: "6-8", count: 54 }, { range: "8-10", count: 32 }],
    students: [{ id: "s1", name: "Nguyễn Minh Anh", score: 8.5, correct: 43, wrong: 7, status: "GRADED" }, { id: "s2", name: "Trần Thu Hà", score: 6.8, correct: 34, wrong: 16, status: "GRADED" }],
  })),
];
