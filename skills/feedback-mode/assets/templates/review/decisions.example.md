# docs への判断

<!-- 置き場は .claude/feedback-decisions/<領域>.md（git に入れる）。形は references/review/decisions.md -->

## 直さない: 指示語「これらは」
- decision: 直さない
- file: docs/guides/comparison.md
- quote: これらは **排他的とは限りません**
- rule: 指示語ではなく指すものを書く
- decided: 2026-11-10 release-check-01 #123
- by: 人（Review）

人の一言: 前の文で並べているので通じる

## 後で: API リファレンスに例が無い
- decision: 後で
- file: docs/reference/**
- decided: 2026-11-10 release-check-01 #123
- by: 人（チャット）
- revisit: v2 のリファレンス自動生成が入ったら

## 方針: 「このページの目的」のコメント
- decision: 方針
- file: docs/**
- match: 目的の?コメント
- decided: 2026-11-10 release-check-01 #123
- by: 人（チャット）

docs のページ先頭に目的のコメントを求める指摘は、言い方が違っても出さない。
