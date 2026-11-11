---
title: v1.2.0 リリース前チェック（docs の文言）
base: 1a7a072
---

## [p7] 設定の例が古い書き方のまま
- file: docs/guides/setup.md
- line: 42-45
- quote: import foo.legacy.Config
- impact: あり: 写すとビルドが失敗する
- recommend: 直してもらう 9, 直さない 1

v1.2 で `foo.legacy` は消えた。例を写した利用者のビルドが落ちる。
- 直し方の案: `import foo.Config` にする

## [p3] 指示語「これらは」
- file: docs/guides/comparison.md
- line: 16
- quote: これらは **排他的とは限りません**
- impact: なし: 意味は通じる
- recommend: 直さない 6, 直してもらう 4

指示語ではなく指すものを書く、の決まりに当たる。前の文で並べているので通じる、とも読める。

## [p3] 指示語「これらは」
- file: docs/guides/faq.md
- line: 140
- quote: これらは
- impact: なし
- recommend: 直さない 6, 直してもらう 4

同じ指摘: comparison.md L16。1 件 = 1 か所なので場所ごとに分けている（同じ見出しなので id は 2 件目に -2 が付く）。

## [p2] 解決済みの例（人が「直さない」を押した）
- file: docs/guides/setup.md
- quote: 推奨されます
- impact: なし
- recommend: 直さない 7, 直してもらう 3
- status: resolved
- resolved-at: 2026-11-10T09:30:00.000Z
- note: 直さない: この節だけは推奨の度合いを残したい

`- status:` から下は UI か `review.mjs resolve` が書く。agent は decisions.py record-review で写してから remove する。
