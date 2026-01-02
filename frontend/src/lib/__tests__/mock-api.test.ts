import { describe, it, expect } from "vitest";
import { api } from "@/lib/mock-api";

describe("mock api", () => {
  it("logs in admin with admin role", async () => {
    const { user } = await api.auth.login("admin", "admin");
    expect(user.role).toBe("admin");
  });

  it("check-in increases credits within configured range", async () => {
    const before = await api.user.getProfile();
    const result = await api.user.checkIn();
    const after = await api.user.getProfile();

    expect(result.points).toBeGreaterThanOrEqual(10);
    expect(result.points).toBeLessThanOrEqual(50);
    expect(after.credits).toBe(result.newTotal);
    expect(after.credits).toBeGreaterThan(before.credits);
  });
});
