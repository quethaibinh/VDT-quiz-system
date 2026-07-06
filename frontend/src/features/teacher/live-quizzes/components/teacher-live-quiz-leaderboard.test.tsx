import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TeacherLiveQuizLeaderboard, type TeacherQuizRankEntry } from "./teacher-live-quiz-leaderboard";

const entries: TeacherQuizRankEntry[] = [
  { id: "rank-1", rank: 1, name: "Champion Student", score: 9.5 },
  { id: "rank-2", rank: 2, name: "Runner Student", score: 9 },
  { id: "rank-3", rank: 3, name: "Third Student", score: 8 },
];

describe("TeacherLiveQuizLeaderboard", () => {
  it("renders podium in #2 - #1 - #3 visual order", () => {
    render(
      <TeacherLiveQuizLeaderboard
        leaderboard={entries}
        metrics={[{ label: "Tong hoc sinh", value: "3" }]}
        progressRows={entries}
      />,
    );

    const podium = screen.getByLabelText("Top leaderboard podium");
    const names = within(podium).getAllByText(/Student$/).map((element) => element.textContent);

    expect(names).toEqual(["Runner Student", "Champion Student", "Third Student"]);
  });
});
