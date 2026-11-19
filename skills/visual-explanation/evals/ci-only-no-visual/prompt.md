---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

CI の Gradle キャッシュの key に libs.versions.toml を足しただけの PR を出す。visual-explanation のやり方に沿って PR 本文を body.md に書いて、決まりに沿ってるか検証までして。
差分は change.diff にある。本文の節は「## チケット / ## 何をしたか / ## 確かめ方 / ## 見てほしい点」でお願い。

Input files: .github/workflows/ci.yml, change.diff
