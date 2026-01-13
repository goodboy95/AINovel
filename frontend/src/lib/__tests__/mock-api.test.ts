import { describe, it, expect, vi, beforeEach } from "vitest";
import { api } from "@/lib/mock-api";

describe("mock api", () => {
  beforeEach(() => {
    const store: Record<string, string> = {};
    vi.stubGlobal("localStorage", {
      getItem: (k: string) => (k in store ? store[k] : null),
      setItem: (k: string, v: string) => {
        store[k] = String(v);
      },
      removeItem: (k: string) => {
        delete store[k];
      },
      clear: () => {
        Object.keys(store).forEach((k) => delete store[k]);
      },
    });
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: any, init?: any) => {
        const u = String(url);
        if (u.endsWith("/api/v1/user/profile")) {
          expect(init?.headers?.get?.("Authorization") || init?.headers?.Authorization).toContain("Bearer t");
          return new Response(
            JSON.stringify({
              id: "u1",
              username: "admin",
              email: "admin@example.com",
              role: "admin",
              credits: 999,
              isBanned: false,
              lastCheckIn: null,
            }),
            { status: 200, headers: { "content-type": "application/json" } }
          );
        }
        return new Response("Not Found", { status: 404 });
      })
    );
  });

  it("reads profile from token in localStorage", async () => {
    localStorage.setItem("token", "t");
    const user = await api.user.getProfile();
    expect(user.role).toBe("admin");
    expect(user.username).toBe("admin");
  });
});
