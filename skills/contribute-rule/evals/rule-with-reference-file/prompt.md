---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

CLAUDE.md の「Git 運用」の規約を、テンプレートの `docs/commit-message.md` ごと TBSten/skills の rule にしたい。名前は `git-commit-convention`、group は Git / GitHub。rule をインストールしたプロジェクトでもテンプレートを同じ `docs/commit-message.md` で参照できるようにしてほしい。

TBSten/skills は `./skills-repo` に clone 済み。gh 未ログインなので push / PR 作成はしなくていい。ファイル作成と README 更新、セルフレビューまでやって。途中の確認は不要。

Input files: CLAUDE.md, docs/commit-message.md, skills-repo/README.md, skills-repo/README.ja.md
