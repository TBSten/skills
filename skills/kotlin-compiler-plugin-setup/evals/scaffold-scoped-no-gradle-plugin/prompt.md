---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

null チェックを自動挿入する compiler plugin の PoC を作りたいです。まずは kotlinc に直接 -Xplugin で渡して試すだけなので、Gradle plugin のラッパーと integration test のモジュールは要りません。compiler-plugin 本体 (unit test 付き) と runtime モジュールだけの構成で ./null-guard にセットアップしてください。

- rootProject 名: null-guard
- groupId: io.github.hoge.nullguard
- compiler plugin ID は groupId と分けて `io.github.hoge.nullguard.compiler` にしたい
- Kotlin のバージョンはデフォルトでOK

ビルドの実行はしなくていいです。
