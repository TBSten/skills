# kmp-snapshot-testing-setup プロンプト

[English](./kmp-snapshot-testing-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[kmp-snapshot-testing-setup スキル](../skills/kmp-snapshot-testing-setup.ja.md) の一回限りプロンプト版。スキルをインストールせずに、Kotlin Multiplatform + Compose プロジェクトへスナップショットテスト基盤 (convention plugins、Kotest PBT 基底クラス、snapshot-diff シェルスクリプト) をセットアップする。

## 実行方法

以下を Claude Code (または任意のコーディングエージェント) に貼り付ける:

```
https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kmp-snapshot-testing-setup/PROMPT.md を取得して、その指示に従って実行して
```

## 何をするか

- skills リポジトリを sparse clone し、同梱の `scripts/install.sh` をそのまま実行する
  (必須: `--project` / `--package`、任意: `--module-path`, `--ui-module-path`,
  `--skip-compose`, `--dry-run`, `--force`)。冪等な script が以下を行う:
  - `gradle/libs.versions.toml` に Kotest / Turbine / kotlinx-serialization / coroutines-test のエントリを追加
  - build-logic の convention plugins (`convention-kmp-test`, `convention-kmp-snapshot-testing`, `SnapshotReportTask`) を ProjectConfig FQCN / モジュールパス置換付きでコピー
  - コアテスト基盤モジュール (build.gradle.kts + 26 ソース: `shouldMatchSnapshot`, `StateHolderSnapshotPbtSpec`, `LogicSnapshotPbtSpec`, `KotlinCodeFormat`, PBT ユーティリティ) と Compose テストモジュール (build.gradle.kts + `ComposeSnapshotPbtSpec`, `runComposableSnapshotTest`) を作成し、`settings.gradle.kts` の include 追記とパッケージ置換まで行う
  - スナップショット差分ワークフロー用シェルスクリプトを配置 (`tools/snapshot-diff.sh` + step スクリプト)
  - ルート `build.gradle.kts` に `cleanSnapshotOutputDir` タスクを登録
  - 末尾に結果 JSON を 1 行出力 (conflicts / warnings / manual follow-ups)
- その後 AI エージェントが JSON をレビューし、プロジェクト固有のフォローアップ (build-logic への serialization plugin classpath 追加、`convention-kmp` の読み替え、`AppTheme` / `WithTestGraph` の調整) を行い、対象モジュールへプラグインを適用、`./gradlew compileKotlinJvm` でビルド確認

## 参照ファイル

プロンプトはローカルのスキルインストールの代わりに、以下を GitHub から取得する:

- [skills/kmp-snapshot-testing-setup/scripts/install.sh](https://github.com/TBSten/skills/blob/main/skills/kmp-snapshot-testing-setup/scripts/install.sh) — 以下すべての配置・置換を行うインストールスクリプト
- [skills/kmp-snapshot-testing-setup/example/build-logic/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/build-logic) — convention plugins とレポートタスク
- [skills/kmp-snapshot-testing-setup/example/core-testing-snapshot/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/core-testing-snapshot) — コアテスト基盤モジュールの build ファイルテンプレート + ソース (26 ファイル)
- [skills/kmp-snapshot-testing-setup/example/ui-core-testing/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/ui-core-testing) — Compose PBT テストモジュールの build ファイルテンプレート + ソース
- [skills/kmp-snapshot-testing-setup/example/tools/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/tools) — snapshot-diff シェルスクリプト
- [skills/kmp-snapshot-testing-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/references) — アーキテクチャドキュメント

## 関連

- スキル版: [skills/kmp-snapshot-testing-setup](../skills/kmp-snapshot-testing-setup.ja.md) — `gh skill install tbsten/skills kmp-snapshot-testing-setup` でインストール
