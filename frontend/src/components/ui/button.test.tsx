import { render, screen } from "@testing-library/react";
import { expect, test } from "vitest";
import { Button } from "./button";

test("renders loading state and disables action", () => {
  render(<Button loading>Import</Button>);
  expect(screen.getByRole("button", { name: "Đang xử lý..." })).toBeDisabled();
});
