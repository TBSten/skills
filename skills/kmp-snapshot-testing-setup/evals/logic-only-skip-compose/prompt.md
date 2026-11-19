---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

shop-pricing は UI を持たない KMP ライブラリ (jvm + js) で、Compose は使っていません。
`DiscountCalculator` みたいな値計算のロジックを、Kotest の property-based testing + shouldMatchSnapshot でスナップショットテストしたいので、テスト基盤を入れてください。

- convention plugin は buildSrc にあります
- テスト基盤モジュールは `core/testing/snapshot` ではなく `testing/snapshot` に置きたい
- パッケージは `io.github.shop.testing`
- `:pricing` に適用してください

Gradle の実行はしなくて大丈夫です。

Input files: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, buildSrc/, pricing/
