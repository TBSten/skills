---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

このリポジトリに Markdown ベースのローカルチケット管理を導入して、早速バグを 1 件起票しておいて。

内容: ログイン画面でパスワードに絵文字 (例: `pass🔑word`) を含めると `POST /api/login` が 500 を返す。普通の英数字パスワードなら 200。サーバー側でハッシュ前に NFC 正規化してないのが怪しい気がしてる。直し方はクライアントで弾くかサーバーで正規化するかで迷ってる。

Input files: package.json, .gitignore, src/login.ts
