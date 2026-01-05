import { describe, expect, it } from "vitest";
import { sha256HexFallback } from "./sha256";

describe("sha256HexFallback", () => {
  it("hashes known vectors", () => {
    expect(sha256HexFallback("")).toBe(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    );
    expect(sha256HexFallback("abc")).toBe(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
    );
  });
});

