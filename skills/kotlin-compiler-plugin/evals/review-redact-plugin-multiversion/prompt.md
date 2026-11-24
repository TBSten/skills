---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill]
---

OSS で公開予定の Kotlin compiler plugin `redact-kt` のレビューをお願いしたいです。`@Redact` を付けたプロパティを data class の `toString()` で `██` に置き換えるプラグインで、1 つの jar で Kotlin 2.2.x 〜 2.3.x をサポートするために compat module 構成にしています。

特に「複数 Kotlin バージョン対応の設計がちゃんとしているか」と「FIR/IR の使い方が妥当か」を見てほしいです。問題点と直し方をまとめてください (コードの修正はまだしなくていいです)。

Input files: settings.gradle.kts, compiler-plugin/, compat/, compat-k220/, compat-k230/, .github/workflows/test.yml
