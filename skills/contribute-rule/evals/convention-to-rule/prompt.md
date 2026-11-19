---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

CLAUDE.md の「Repository 層の戻り値規約」、コードを書くときに常に守らせたい規約なので、TBSten/skills に rule として登録したい。名前は `kmp-repository-outcome` で。

TBSten/skills は `./skills-repo` に clone 済み。この環境は gh にログインしていないので push と PR 作成はしなくていい。PR のタイトルと本文は `./pr-draft.md` に書いて。途中の確認は全部 OK なので、聞き返さずに最後まで進めて。

Input files: CLAUDE.md, skills-repo/README.md, skills-repo/README.ja.md
