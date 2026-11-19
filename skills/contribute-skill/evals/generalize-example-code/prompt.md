---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

`core/src/main/kotlin/com/acmepay/core/flow/RetryWithBackoff.kt` の Flow リトライ拡張、他のプロジェクトでも使い回したいので TBSten/skills に skill として contribute したい。skill 名は `flow-retry-backoff`、サンプルコードとテストも example として同梱してほしい (利用箇所は feature/search を見て)。

TBSten/skills は `./skills-repo` に clone 済み。gh は未ログインなので push / PR 作成はしないで、ファイル作成とセルフレビューまででいい。途中の確認は全部 OK なので聞き返さずに進めて、最後に何をどう置き換えたかを教えて。

Input files: core/src/main/kotlin/com/acmepay/core/flow/RetryWithBackoff.kt, core/src/test/kotlin/com/acmepay/core/flow/RetryWithBackoffTest.kt, feature/src/main/kotlin/com/acmepay/feature/search/SearchRepository.kt, skills-repo/README.md, skills-repo/README.ja.md
