---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

自作の Kotlin compiler plugin (`trace-kt`、現在 v1.4.2 をリリース済み) のサポート Kotlin バージョンを更新したいです。

- Kotlin 2.4.0 (stable) をサポートに追加したい。手元で `./scripts/compiler-plugin-test.sh 2.4.0` を回したら、既存の compat-k240_beta2 のままで全テスト GREEN でした
- 逆に Kotlin 2.1.20 / 2.1.21 はもうサポートから外したい

このリポジトリで必要なファイル更新をやってください。あと、この変更を入れた次のリリースのバージョン番号をどう上げるべきかも教えてください。

Input files: settings.gradle.kts, gradle.properties, scripts/supported-kotlin-versions.txt, README.md, README.ja.md, compiler-plugin/
