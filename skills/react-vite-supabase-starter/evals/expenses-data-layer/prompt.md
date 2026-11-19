---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

react-vite-supabase-starter で作った kakeibo-web に、支出の一覧・追加・削除ができるページを足したいです。

Supabase には `expenses` テーブル (id uuid, user_id uuid, amount integer, category text, spent_at date) があり、RLS で自分の行だけ見える設定です。

- `/expenses` に一覧 (spent_at の新しい順) と追加フォーム (金額・カテゴリ・日付)
- 各行に削除ボタン
- 追加・削除したら一覧が自動で更新されて、トーストで結果が出るようにしたい
- ログインしていないと見られないページにしてください

依存のインストールやビルドは不要で、コードの追加だけお願いします。

Input files: package.json, pnpm-workspace.yaml, apps/web/package.json, apps/web/src/ (lib/, auth/, data/auth/, pages/home.tsx, router.tsx, components/)
