---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

IntelliJ プラグイン (Jewel の tool window + Kotlin Analysis API で `@ScreenSpec` 注釈を解析するやつ) の機能テストが通らなくて困っています。

`src/test/kotlin/io/github/hoge/specviewer/SpecAnalysisTest.kt` を `./gradlew test` で回すと:
1. `analyze(ktClass) { ... }` のところで、Analysis API が K2 で動いていない (K1 モード扱い) っぽいエラーになる
2. 試しに analyze 部分をコメントアウトすると、今度はアサーションは通るのに tearDown で `TestLoggerFactory$TestLoggerAssertionError` が出て落ちる。ログを見ると bundled の Vue / LSP プラグインの VFS listener 初期化失敗の logged error でした

テストが通るように build 設定・plugin.xml・テストコードを直してください。Gradle の実行はこちらでやります。

Input files: build.gradle.kts, src/main/resources/META-INF/plugin.xml, src/test/kotlin/io/github/hoge/specviewer/SpecAnalysisTest.kt
