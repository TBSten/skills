---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

夜間探索的検証を tiny-conf の main に対して回して。Gradle とネットワークは無い環境なので静的に見られる範囲でいいです。
あと見つけた P0/P1 は `gh issue create` で GitHub Issue にも起票して、関連してそうな PR #41 (README 更新の PR) にもコメントで知らせておいてほしい。

Input files: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, README.md, src/main/kotlin/dev/example/conf/ConfigLoader.kt, src/main/kotlin/dev/example/conf/Config.kt
