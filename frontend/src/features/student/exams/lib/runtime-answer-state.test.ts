import { describe, it, expect } from "vitest";
import { initializeAnswers, toggleSingleChoice, toggleMultipleChoice } from "./runtime-answer-state";
import type { StudentQuestion, StudentAnswer } from "../model/student-exam-contracts";

describe("runtime-answer-state helpers", () => {
  it("khoi tao answers map rong khi khong co savedAnswers", () => {
    const questions: StudentQuestion[] = [
      {
        questionId: "q1",
        difficulty: "EASY",
        type: "SINGLE_CHOICE",
        content: "Question 1",
        contentFormat: "TEXT",
        score: 1,
        options: [],
      },
    ];

    const result = initializeAnswers(questions, []);
    expect(result["q1"]).toBeDefined();
    expect(result["q1"].selectedOptionIds).toEqual([]);
    expect(result["q1"].markedForReview).toBe(false);
  });

  it("merge thanh cong savedAnswers tu backend vao de thi", () => {
    const questions: StudentQuestion[] = [
      {
        questionId: "q1",
        difficulty: "EASY",
        type: "SINGLE_CHOICE",
        content: "Question 1",
        contentFormat: "TEXT",
        score: 1,
        options: [],
      },
      {
        questionId: "q2",
        difficulty: "MEDIUM",
        type: "MULTIPLE_CHOICE",
        content: "Question 2",
        contentFormat: "TEXT",
        score: 1,
        options: [],
      },
    ];

    const saved: StudentAnswer[] = [
      {
        questionId: "q1",
        selectedOptionIds: ["opt1"],
        answerText: null,
        markedForReview: true,
      },
    ];

    const result = initializeAnswers(questions, saved);
    expect(result["q1"].selectedOptionIds).toEqual(["opt1"]);
    expect(result["q1"].markedForReview).toBe(true);
    expect(result["q2"].selectedOptionIds).toEqual([]);
    expect(result["q2"].markedForReview).toBe(false);
  });

  it("toggles single choice dung cach", () => {
    const result = toggleSingleChoice("opt2");
    expect(result).toEqual(["opt2"]);
  });

  it("toggles multiple choice dung cach", () => {
    // Neu chua co thi append vao
    const res1 = toggleMultipleChoice(["opt1"], "opt2");
    expect(res1).toEqual(["opt1", "opt2"]);

    // Neu da co thi loai bo di
    const res2 = toggleMultipleChoice(["opt1", "opt2"], "opt1");
    expect(res2).toEqual(["opt2"]);
  });
});
