import { describe, expect, it } from "vitest";
import { getTeacherSection, teacherNavigationItems } from "@/features/teacher/layout/teacher-navigation";

describe("teacher navigation", () => {
  it("keeps exactly three global destinations", () => {
    expect(teacherNavigationItems.map((item) => item.label)).toEqual(["Môn học", "Ca thi", "Kết quả"]);
    expect(teacherNavigationItems.map((item) => item.path)).toEqual([
      "/teacher/subjects",
      "/teacher/exams",
      "/teacher/results",
    ]);
  });

  it.each([
    ["/teacher/subjects", "subjects"],
    ["/teacher/subjects/subject-1/questions", "subjects"],
    ["/teacher/subjects/subject-1/collections/collection-1", "subjects"],
    ["/teacher/exams", "exams"],
    ["/teacher/subjects/subject-1/exams", "exams"],
    ["/teacher/exams/exam-1/monitor", "exams"],
    ["/teacher/results", "results"],
    ["/teacher/subjects/subject-1/results", "results"],
    ["/teacher/exams/exam-1/results", "results"],
  ] as const)("classifies %s as %s", (pathname, section) => {
    expect(getTeacherSection(pathname)).toBe(section);
  });
});
