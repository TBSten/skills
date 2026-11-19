---
tags: [skill-eval]
max_turns: 15
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

CLAUDE.md の「ViewModel の規約」を TBSten/skills に prompt として登録したい。名前は `viewmodel-uistate-prompt` あたりで。TBSten/skills は `./skills-repo` に clone 済み、gh 未ログインなので push / PR はまだしなくていい。

Input files: CLAUDE.md, skills-repo/README.md, skills-repo/README.ja.md
