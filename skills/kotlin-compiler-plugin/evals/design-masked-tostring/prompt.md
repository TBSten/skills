---
tags: [skill-eval]
max_turns: 15
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill]
---

compiler plugin の設計を相談させてください。

やりたいこと: data class のプロパティに `@Masked` を付けると、自動生成される `toString()` の出力でその値が `***` に置き換わるようにしたい (ログに個人情報が出ないように)。あと、`@Masked` を data class 以外のクラスのプロパティに付けたらコンパイルエラーにしたいです。

Kotlin 2.3 (K2) 前提です。これって FIR と IR のどっちで何をやるべき？ 使う Extension Point と、参考にできる既存プラグインがあれば教えてください。まだ実装はしなくていいです。
