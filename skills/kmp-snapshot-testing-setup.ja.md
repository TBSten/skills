# KMP Snapshot Testing Setup スキル

[English](./kmp-snapshot-testing-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Multiplatform + Compose プロジェクトにスナップショットテスト基盤をセットアップする [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

## クイックスタート

### 1. スキルをインストール:

```bash
gh skill install tbsten/skills kmp-snapshot-testing-setup
```

### 2. AI エージェントに依頼:

```
スナップショットテスト基盤をセットアップして。
```

## インストールの仕組み

ファイル配置・置換は `scripts/install.sh` が一括で行う
(必須: `--project` / `--package`、任意: `--module-path`, `--ui-module-path`,
`--skip-compose`, `--dry-run`, `--force`)。script は冪等で、再実行しても
catalog エントリや `settings.gradle.kts` の include が二重追記されることはなく、
`--force` なしで既存ファイルを上書きしない。実行結果は末尾に 1 行 JSON
(conflicts / warnings / manual follow-ups) で出力される。
その後、AI エージェントがプロジェクト固有のフォローアップ
(build-logic への serialization plugin classpath 追加、`AppTheme` / `WithTestGraph` の調整、
catalog alias の整合) とビルド確認を行う。

## セットアップされるもの

### ビルドロジック (Convention Plugins)

| ファイル | 説明 |
|---|---|
| `convention-kmp-test.gradle.kts` | Kotest, Turbine, coroutines-test を commonTest/jvmTest に追加 |
| `convention-kmp-snapshot-testing.gradle.kts` | jvmSnapshotTest コンパイル、Record/Verify/Report タスクを登録 |
| `SnapshotReportTask.kt` | 差分レポート生成 (JSON + Markdown + HTML) |

### テストモジュール

| モジュール | 主要コンポーネント |
|---|---|
| `core/testing/snapshot` | build.gradle.kts (テンプレート), ProjectConfig, shouldMatchSnapshot, StateHolderSnapshotPbtSpec, LogicSnapshotPbtSpec, KotlinCodeFormat, PBT ユーティリティ |
| `ui/core/testing` | build.gradle.kts (テンプレート), ComposeSnapshotPbtSpec, runComposableSnapshotTest |

どちらのモジュールも install script が `settings.gradle.kts` への include 追記と
version catalog (`gradle/libs.versions.toml` の kotest / turbine / kotlinx-serialization /
coroutines-test エントリ) への追記まで行う。

### シェルスクリプト

| スクリプト | 説明 |
|---|---|
| `tools/snapshot-diff.sh` | オーケストレーター: worktree ベースの record → verify → report |
| `tools/snapshot-diff/step1-5` | diff ワークフローの各ステップ |

## セットアップ後の使い方

```bash
# main ブランチとのスナップショット差分を確認
./tools/snapshot-diff.sh -before=main

# PBT 反復数を減らして高速チェック
./tools/snapshot-diff.sh -before=main -pbt-iteration=10

# build/snapshots/result.html でビジュアルレポートを確認
```
