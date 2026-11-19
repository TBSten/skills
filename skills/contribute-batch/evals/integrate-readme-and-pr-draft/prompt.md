---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

さっきの知見の一括 contribute の続き。仕分けは承認済みで、subagent による成果物の作成も終わってる (`./skills-repo` が作業 branch `feat/contribute-batch-compose-knowledge` の clone)。

- skill: `compose-screenshot-update` → `skills-repo/skills/compose-screenshot-update/`
- rule: `compose-naming` → `skills-repo/rules/compose-naming/`
- 見送り: 社内 VPN・検証環境の接続手順 (プロジェクト固有すぎるため)

ここからオーケストレータとしての統合をやって: README.md / README.ja.md の更新、セルフレビュー、それから commit の分け方 (メッセージ案) と PR のタイトル・本文を `./pr-draft.md` に書く。gh 未ログインなので commit / push / PR 作成はしないで。

Input files: skills-repo/README.md, skills-repo/README.ja.md, skills-repo/skills/compose-screenshot-update/SKILL.md, skills-repo/rules/compose-naming/RULE.md, skills-repo/rules/compose-naming.md
