---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

自作の KMP ライブラリ kvstore (GitHub: hoge-dev/kvstore) を Maven Central に公開できるようにしたいです。
公開するのは `:kvstore-core` モジュールだけで、iOS ターゲットも含みます。

- groupId: `dev.hoge.kvstore`
- バージョンは `gradle/libs.versions.toml` の `[versions]` に `kvstore = "0.1.0"` として持たせたい
- description は "Simple Kotlin Multiplatform key-value store" でお願いします
- convention は buildSrc に置いてください

GPG 鍵や GitHub Secrets の登録は後で自分でやるので、今回はリポジトリ内の設定ファイルまでで大丈夫です。ビルドの実行も不要です。

Input files: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, kvstore-core/build.gradle.kts, LICENSE
