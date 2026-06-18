import { describe, expect, test } from "vitest";
import { decodeAuthToken, isExpired } from "@/features/auth/lib/jwt";

function token(payload: Record<string, unknown>) {
  const encode = (value: object) => {
    const bytes = new TextEncoder().encode(JSON.stringify(value));
    const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join("");
    return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
  };
  return `${encode({ alg: "none", typ: "JWT" })}.${encode(payload)}.`;
}

describe("decodeAuthToken", () => {
  test("reads teacher claims", () => {
    const jwt = token({
      userId: "teacher-1",
      userRole: "TEACHER",
      username: "GV001",
      fullName: "Nguyễn Văn An",
      exp: Math.floor(Date.now() / 1000) + 3600,
    });
    const claims = decodeAuthToken(jwt);
    expect(claims.userRole).toBe("TEACHER");
    expect(claims.fullName).toBe("Nguyễn Văn An");
    expect(isExpired(claims)).toBe(false);
  });

  test("rejects incomplete token", () => {
    const jwt = token({ username: "GV001", exp: Math.floor(Date.now() / 1000) + 3600 });
    expect(() => decodeAuthToken(jwt)).toThrow();
  });
});
