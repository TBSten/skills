import { cn } from "./utils";

// テスト環境 (vitest + globals) の動作確認を兼ねたサンプルテスト
describe("cn", () => {
  it("複数のクラス名を結合する", () => {
    expect(cn("flex", "items-center")).toBe("flex items-center");
  });

  it("競合する Tailwind クラスは後勝ちでマージされる", () => {
    expect(cn("p-2", "p-4")).toBe("p-4");
  });

  it("falsy な値は無視される", () => {
    expect(cn("flex", false, undefined, null, "gap-2")).toBe("flex gap-2");
  });
});
