# kmp-snapshot-testing-setup プロンプト

[English](./kmp-snapshot-testing-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[kmp-snapshot-testing-setup スキル](../skills/kmp-snapshot-testing-setup.ja.md) の一回限りプロンプト版。スキルをインストールせずに、Kotlin Multiplatform + Compose プロジェクトへスナップショットテスト基盤 (convention plugins、Kotest PBT 基底クラス、snapshot-diff シェルスクリプト) をセットアップする。

## 実行方法

以下を Claude Code (または任意のコーディングエージェント) に貼り付ける:

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kmp-snapshot-testing-setup/PROMPT.md を取得して、その指示に従って実行して
```

## 何をするか

- `gradle/libs.versions.toml` に Kotest / Turbine / coroutines-test のエントリを追加
- build-logic の convention plugins を作成 (`convention-kmp-test`, `convention-kmp-snapshot-testing`, `SnapshotReportTask`)
- コアテスト基盤モジュールを作成 (`shouldMatchSnapshot`, `StateHolderSnapshotPbtSpec`, `LogicSnapshotPbtSpec`, `KotlinCodeFormat`, PBT ユーティリティ)
- Compose テストモジュールを作成 (`ComposeSnapshotPbtSpec`, `runComposableSnapshotTest`)
- スナップショット差分ワークフロー用シェルスクリプトを配置 (`tools/snapshot-diff.sh` + step スクリプト)
- ルート `build.gradle.kts` に `cleanSnapshotOutputDir` タスクを登録し、対象モジュールへプラグインを適用、`./gradlew compileKotlinJvm` でビルド確認

## 参照ファイル

プロンプトはローカルのスキルインストールの代わりに、以下を GitHub から取得する:

- [skills/kmp-snapshot-testing-setup/example/build-logic/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/build-logic) — convention plugins とレポートタスク
- [skills/kmp-snapshot-testing-setup/example/core-testing-snapshot/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/core-testing-snapshot) — コアテスト基盤モジュールのソース (25 ファイル)
- [skills/kmp-snapshot-testing-setup/example/ui-core-testing/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/ui-core-testing) — Compose PBT テストモジュールのソース
- [skills/kmp-snapshot-testing-setup/example/tools/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/tools) — snapshot-diff シェルスクリプト
- [skills/kmp-snapshot-testing-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/references) — アーキテクチャドキュメント

## 関連

- スキル版: [skills/kmp-snapshot-testing-setup](../skills/kmp-snapshot-testing-setup.ja.md) — `gh skill install tbsten/skills kmp-snapshot-testing-setup` でインストール
