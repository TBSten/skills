---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

JSON Schema のバリデーションライブラリ `schema-kt` を Kotlin Multiplatform で作りたいです。CLI からも使いたいので、Android / iOS / JVM / JS / Wasm に加えて Linux・Windows・macOS のネイティブまで、出せるターゲットはできるだけ全部カバーしたいです。

- 生成先: ./schema-kt
- パッケージ: io.github.hoge.schema
- GitHub: hoge/schema-kt
- 含めるものはデフォルト全部でOK

あと、ゆくゆくは `@JsonSchema` を付けた data class から schema 定義を生成する KSP processor も同じリポジトリで提供したいと思っています。それも含めて土台を作ってもらえますか？ ビルド確認は今回はしなくていいです。
