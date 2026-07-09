import { expect, test } from "vitest";
import { unwrap } from "@/lib/http/unwrap";

test("unwraps the backend success envelope", () => {
  expect(unwrap({ timestamp: "now", status: 200, message: "Success", data: { id: 1 } })).toEqual({ id: 1 });
});

test("keeps an unwrapped payload", () => {
  expect(unwrap([{ id: 1 }])).toEqual([{ id: 1 }]);
});
