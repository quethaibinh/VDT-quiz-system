import { describe, expect, it } from "vitest";
import { getTeacherSection, teacherNavigationItems } from "@/features/teacher/layout/teacher-navigation";

describe("teacher navigation", () => {
  it("keeps the expected global destinations", () => {
    expect(teacherNavigationItems.map((item) => item.path)).toEqual([
      "/teacher/subjects",
      "/teacher/exams",
      "/teacher/live-quizzes",
      "/teacher/monitoring",
      "/teacher/results",
      "/teacher/profile",
    ]);
  });

  it.each([
    ["/teacher/subjects", "subjects"],
    ["/teacher/subjects/subject-1/questions", "subjects"],
    ["/teacher/subjects/subject-1/collections/collection-1", "subjects"],
    ["/teacher/exams", "exams"],
    ["/teacher/subjects/subject-1/exams", "exams"],
    ["/teacher/live-quizzes", "live-quizzes"],
    ["/teacher/subjects/subject-1/live-quizzes", "live-quizzes"],
    ["/teacher/live-quizzes/room-1/room", "live-quizzes"],
    ["/teacher/monitoring", "monitoring"],
    ["/teacher/exams/exam-1/monitor", "monitoring"],
    ["/teacher/results", "results"],
    ["/teacher/subjects/subject-1/results", "results"],
    ["/teacher/exams/exam-1/results", "results"],
    ["/teacher/profile", "profile"],
  ] as const)("classifies %s as %s", (pathname, section) => {
    expect(getTeacherSection(pathname)).toBe(section);
  });
});
