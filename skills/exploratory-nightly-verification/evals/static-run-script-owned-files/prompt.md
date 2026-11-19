---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

tiny-conf (Kotlin/JVM の小さい設定ローダーのライブラリ) の main を夜間探索的検証してほしい。
今回の環境は Gradle もネットワークも使えないので、cat3 (build/test) と cat4 (upstream) は「実行できなかった」扱いでいいです。
cat1 の静的解析と cat2 の README ↔ コードの整合チェックを中心に、見つけたものを issue Markdown に書いて、最後に SUMMARY まで作ってください。

Input files: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, README.md, src/main/kotlin/dev/example/conf/ConfigLoader.kt, src/main/kotlin/dev/example/conf/Config.kt
