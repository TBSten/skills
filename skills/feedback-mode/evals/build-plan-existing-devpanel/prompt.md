---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

この warehouse-web (倉庫スタッフ向けの入出庫管理アプリ) の debug ビルドに、フィードバックモードを入れたいです。
画面の部品を選んでコメントすると、画面の状態やログ付きの記録がリポジトリ内に溜まって、あとで Claude Code にまとめて直してもらう、というやつ。

いきなりコードは書かずに、まず導入プランを出してください。OK したら実装に進んでもらいます。

Input files: package.json, README.md, src/main.ts, src/app.ts, src/debug/DevPanel.ts, src/lib/logger.ts
