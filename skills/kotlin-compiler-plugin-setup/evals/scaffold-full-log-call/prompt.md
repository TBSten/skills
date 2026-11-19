---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

関数の呼び出しにログを差し込む Kotlin compiler plugin を作り始めたいので、プロジェクトをセットアップしてください。

- プロジェクト名: `log-call`
- groupId / plugin id: `io.github.hoge.logcall`
- Kotlin: 2.3.20
- 構成はフルセット (compiler-plugin・Gradle plugin・runtime・テスト・integration test 全部) でお願いします

このカレントディレクトリ直下に生成してください。Gradle のビルド確認は CI 環境が遅いので今回は実行しなくて大丈夫です (生成物の確認まででOK)。
