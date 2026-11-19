---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

tiny-csv (JVM のみの Kotlin ライブラリ) を Maven Central に出したいので、publish の設定を入れてください。

- 公開するのは `:core` だけ。`:sample` はデモアプリなので公開しません
- groupId とバージョンはもう `gradle.properties` の `GROUP` / `VERSION_NAME` に書いてあるのでそれを SSoT にしたい
- convention plugin は既存の `build-logic` (included build) に置いてください
- POM の description は "A tiny CSV parser for Kotlin/JVM"

鍵やシークレットの登録はまだやらなくていいです。Gradle の実行も不要です。

Input files: settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, build-logic/build.gradle.kts, build-logic/settings.gradle.kts, core/build.gradle.kts, sample/build.gradle.kts, LICENSE
