---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

acme-todo (KMP + Compose Multiplatform) にスナップショットテストの基盤を導入したいです。
Kotest の PBT で StateHolder と Compose の画面をスナップショットで固めて、PR ごとに snapshot-diff.sh で差分を見られるようにしたい。

- テスト基盤のパッケージは `com.acme.todo.testing`、モジュールの置き場所はデフォルトで OK
- 既に build-logic (included build) に convention-kmp / convention-compose がある
- テーマは `:core:designsystem` の `AcmeTheme`。DI は使っていません
- まずは `:feature:todo` でスナップショットテストを書けるようにしたい

この環境では Gradle のビルドは実行できないので、ファイルの配置と設定まででお願いします。

Input files: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, build-logic/, core/designsystem/, feature/todo/
