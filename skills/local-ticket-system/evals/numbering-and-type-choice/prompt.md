---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

.local/ticket でチケット管理してるプロジェクトです。次の 2 つをチケットにしておいて。

1. 注文一覧の CSV ダウンロードボタンに `aria-label` が無くてスクリーンリーダーで読めないので付ける (ボタン 1 個直すだけ)
2. マルチテナント対応。テナントごとにデータを分けたいけど、DB スキーマ (tenant_id の追加)・API の認可・管理画面のテナント切り替えまで絡むし、まだ要件もふわっとしてる

Input files: .local/ticket/ (既存チケット), package.json
