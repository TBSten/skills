---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

社内の KMP (Android + iOS + JVM サーバ) プロジェクトで、各 feature モジュールに散らばっている API ハンドラクラスを、コンパイル時に :app 側でまとめて取得できる仕組みを Kotlin Compiler Plugin で作りたいです。ServiceLoader のコンパイル時版みたいなイメージ。

決まっていること:
- Kotlin 2.3.0 (K2)
- base package: `io.github.hoge.collector`
- 収集対象のマーカーは `@ApiHandler(tag = "...")`、収集結果を受け取る側は `@CollectHandlers(tag = "...")` を付けた関数 (`fun adminHandlers(): List<KClass<*>>` みたいな形)
- tag は "admin" と "public" の 2 種類で使う予定
- Gradle plugin id は `io.github.hoge.collector`、groupId は `io.github.hoge`

設計の確認は不要なので、このカレントディレクトリに Gradle プロジェクト一式 (モジュール構成・build.gradle.kts・プラグイン本体のソース・テスト) をそのまま生成してください。ビルドの実行は不要です。
