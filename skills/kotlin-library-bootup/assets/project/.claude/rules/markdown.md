---
paths:
  - "**/*.md"
  - "**/*.mdx"
---

# Markdown の en/ja 同期

利用者向けの文書は英語と日本語の 2 本立て。片方を変えたら、**同じ変更でもう片方も更新する**。
後で揃えるつもりで片方だけ直すと、ほぼ確実にずれたまま残る。

| 英語 (正) | 日本語 |
|---|---|
| `README.md` | `README.ja.md` |
<!-- @bootup:if docs-site -->
| `docs/src/content/docs/**/*.mdx` | `docs/src/content/docs/ja/**/*.mdx` (同じ相対パス) |
| `docs/astro.config.mjs` の sidebar `label` | 同じ項目の `translations.ja` |
<!-- @bootup:end -->

- 英語が正。内容が食い違ったら英語に合わせる
- コード例・コマンド・バージョン表記は両方で同一にする (説明文とコメントだけ訳す)
- 見出しを変えたら、`**Contents:**` / `**目次:**` 行のアンカーも直す
- 片方にしか無いページを作らない。訳が間に合わないときも、ページとサイドバー項目は両方に作る

## 対象外 (日本語 1 本 / 英語 1 本でよいもの)

<!-- @bootup:if docs-site -->
- `CLAUDE.md`、`docs/CLAUDE.md`、`.claude/**` — AI エージェント向け。翻訳を作らない
<!-- @bootup:end -->
<!-- @bootup:if !docs-site -->
- `CLAUDE.md`、`.claude/**` — AI エージェント向け。翻訳を作らない
<!-- @bootup:end -->
- `.local/**` — 作業メモ。コミットしない
<!-- @bootup:if docs-site -->
- `*.api` や生成物 (`docs/public/api-docs/**`)
<!-- @bootup:end -->
<!-- @bootup:if !docs-site -->
- `*.api` や生成物
<!-- @bootup:end -->
