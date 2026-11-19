---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

金額計算用の小さな Kotlin ライブラリ `money-kt` を OSS で出したいので、プロジェクトを立ち上げてください。

- 生成先: ./money-kt
- パッケージ / groupId: io.github.hoge.money
- サーバーサイド (Spring / Ktor) でしか使わないので JVM のみでいいです
- docs サイトは README で十分なので要りません。それ以外 (integrationTest・CI・同梱 skill) は入れてください
- GitHub は hoge/money-kt (まだ remote は設定していません)

この環境はネットワークが細くて Gradle が回らないので、ビルド確認 (verify) は手元でやります。生成と中身のレビューまでお願いします。
