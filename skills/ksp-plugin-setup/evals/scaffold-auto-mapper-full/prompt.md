---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

cream と同じ構成で KSP プラグインを新しく作りたいです。`@AutoMapper` を付けた data class から DTO ⇔ ドメインモデルの mapper 関数を生成する processor の予定です。

- プロジェクト名: mapper-gen
- パッケージ / groupId: io.github.hoge.mappergen
- 最初のアノテーション: AutoMapper
- GitHub: hoge/mapper-gen (まだ git init してないので remote はありません)
- それ以外 (test モジュール・CI・規約ファイル) は全部入りでOK

カレントディレクトリにセットアップしてください。Android SDK がこの環境に無いので、Gradle でのビルド確認 (verify) は今回はスキップしてください。
