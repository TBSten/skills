---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

acme-shop (KMP + Compose Multiplatform) で、データ読み込みの状態管理を共通化したいです。
初回読み込みとリフレッシュを区別できて、Loading / Error / Loaded の遷移を持った SimpleLoader を `:ui:core` モジュールに作ってください。

- パッケージは `com.acme.shop.ui.core.loader`
- Compose 用の View / AnimatedView ヘルパーは欲しい
- プログレス表示 (PartialData) は今回は要らないので外してください
- 依存が足りなければ build.gradle.kts に追加してください

Gradle のビルドは実行しなくて大丈夫です。

Input files: settings.gradle.kts, gradle/libs.versions.toml, ui/core/build.gradle.kts, feature/home/build.gradle.kts
