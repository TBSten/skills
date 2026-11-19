---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

KMP プロジェクトの `:shared` モジュールに Tuple のユーティリティを入れたいです。Pair / Triple だと足りない場面が増えてきたので。

- パッケージは `com.acme.app.util.tuple`
- Tuple は最大 8 要素まであれば十分
- toList / Serializer / awaitAll 系 / allNotNullOrNull など全部入りで OK、確認なしで進めてください

この環境には Android SDK が無いので Gradle のビルド確認はスキップしてください。

Input files: settings.gradle.kts, gradle/libs.versions.toml, shared/build.gradle.kts, androidApp/build.gradle.kts
