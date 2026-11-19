---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

report-batch (Kotlin/JVM の単一モジュール) の `DailyReportJob` で、3 つの API を並列に叩いて 1 つ失敗しても残りの結果は使えるようにしたいです。
型安全な awaitAll で Result を返すやつ (AwaitAllCatching) と、nullable をまとめてチェックする allNotNullOrNull だけ欲しいです。
Serializer や toList は要りません。

パッケージは `jp.fuga.batch.tuple` で、Tuple の最大サイズはデフォルトのままで大丈夫です。

Input files: settings.gradle.kts, build.gradle.kts, src/main/kotlin/jp/fuga/batch/job/DailyReportJob.kt, src/main/kotlin/jp/fuga/batch/legacy/Tuples.kt
