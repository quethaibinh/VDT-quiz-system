import { describe, expect, it } from "vitest";
import {
  createInitialBuilderState,
  reconcileStudentIds,
  toExamDraftInput,
  validateBuilderState,
} from "@/features/teacher/exams/components/builder/builder-state";

describe("exam builder state", () => {
  it("khong dua subjectId hoac selectedStudents vao request", () => {
    const state = createInitialBuilderState();
    state.title = "Giữa kỳ";
    state.collectionId = "collection-1";
    state.startAtLocal = "2026-07-01T08:00";
    state.selectedStudents.set("student-1", {
      id: "student-1",
      studentCode: "SV001",
      fullName: "Nguyễn An",
      displayName: "An",
    });

    const request = toExamDraftInput(state);

    expect(request.startAt).toMatch(/(Z|[+-]\d{2}:\d{2})$/);
    expect(request).not.toHaveProperty("subjectId");
    expect(request).not.toHaveProperty("selectedStudents");
  });

  it("chan quota vuot capacity va danh sach hoc sinh rong", () => {
    const state = createInitialBuilderState();
    state.title = "Giữa kỳ";
    state.collectionId = "collection-1";
    state.startAtLocal = "2026-07-01T08:00";
    state.easyCount = 21;

    expect(validateBuilderState(state, { easy: 20, medium: 20, hard: 10 })).toEqual(
      expect.arrayContaining([
        "Vui lòng chọn ít nhất một học sinh.",
        "Số câu dễ vượt quá bộ câu hỏi.",
      ]),
    );
  });

  it("chi tra ve assignment thay doi", () => {
    expect(reconcileStudentIds(["a", "b"], ["b", "c"])).toEqual({
      added: ["c"],
      removed: ["a"],
    });
  });
});
