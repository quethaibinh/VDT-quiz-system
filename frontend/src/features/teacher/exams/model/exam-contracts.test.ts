import type {
  AssignmentMutationResult,
  ExamDetail,
  ExamDraftRequest,
  ExamSummary,
  StudentSummary,
} from "@/features/teacher/exams/model/exam-contracts";
import { expect, test } from "vitest";

test("models backend exam and assignment DTO fields", () => {
  const request = {
    title: "Giua ky",
    description: "De thi thu",
    collectionId: "collection-1",
    easyCount: 10,
    mediumCount: 5,
    hardCount: 2,
    startAt: "2026-06-20T08:00:00+07:00",
    durationMinutes: 60,
    joinBeforeMinutes: 10,
    joinAfterMinutes: 5,
    shuffleQuestions: true,
    shuffleOptions: true,
    showResultPolicy: "AFTER_CLOSED",
    autoSubmit: true,
    requireFullscreen: true,
    maxViolationAllowed: 3,
    handleViolation: "WARN",
  } satisfies ExamDraftRequest;

  expect(request).not.toHaveProperty("subjectId");

  const summary = {
    id: "exam-1",
    code: "EX001",
    title: request.title,
    subjectId: "subject-1",
    subjectName: "Toan",
    collectionId: request.collectionId,
    collectionName: "Bo de 1",
    startAt: request.startAt,
    durationMinutes: 60,
    questionCount: 17,
    assignedCount: 2,
    status: "CANCELLED",
    version: 4,
  } satisfies ExamSummary;

  const detail = {
    ...summary,
    description: request.description,
    easyCount: 10,
    mediumCount: 5,
    hardCount: 2,
    endAt: "2026-06-20T09:00:00+07:00",
    joinBeforeMinutes: 10,
    joinAfterMinutes: 5,
    shuffleQuestions: true,
    shuffleOptions: true,
    showResultPolicy: "AFTER_CLOSED",
    autoSubmit: true,
    requireFullscreen: true,
    maxViolationAllowed: 3,
    handleViolation: "WARN",
  } satisfies ExamDetail;

  const student = {
    id: "student-1",
    studentCode: "SV001",
    fullName: "Nguyen Van A",
    displayName: "Van A",
  } satisfies StudentSummary;

  const mutation = {
    requestedCount: 1,
    assignedCount: 1,
    unchangedCount: 0,
    assignments: [{
      id: "assignment-1",
      studentId: student.id,
      studentCode: student.studentCode,
      studentName: student.fullName,
      status: "ASSIGNED",
      assignedAt: "2026-06-18T10:00:00",
      removedAt: null,
    }],
  } satisfies AssignmentMutationResult;

  expect(detail.status).toBe("CANCELLED");
  expect(mutation.assignments[0].status).toBe("ASSIGNED");
});
