---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

CLAUDE.md にある detekt 導入手順、新しいリポジトリを作るたびに 1 回やるだけのセットアップなので、TBSten/skills にワンショットプロンプトとして登録したい。名前は `detekt-setup` で、group は Kotlin / Android アプリ開発。

TBSten/skills は `./skills-repo` に clone 済み。gh にログインしていないので push と PR 作成はしなくていい。PR のタイトルと本文は `./pr-draft.md` に書いておいて。途中の確認は全部 OK なので聞き返さずに最後まで進めて。

Input files: CLAUDE.md, skills-repo/README.md, skills-repo/README.ja.md
