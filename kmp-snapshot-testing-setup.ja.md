# KMP Snapshot Testing Setup スキル

[English](./kmp-snapshot-testing-setup.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Multiplatform + Compose プロジェクトにスナップショットテスト基盤をセットアップする [Claude Code](https://docs.anthropic.com/en/docs/claude-code) スキル。

## クイックスタート

### 1. スキルをインストール:

```bash
npx skills add tbsten/skills --skill kmp-snapshot-testing-setup
```

### 2. AI エージェントに依頼:

```
スナップショットテスト基盤をセットアップして。
```

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
| `core/testing/snapshot` | ProjectConfig, shouldMatchSnapshot, StateHolderSnapshotPbtSpec, LogicSnapshotPbtSpec, KotlinCodeFormat, PBT ユーティリティ |
| `ui/core/testing` | ComposeSnapshotPbtSpec, runComposableSnapshotTest |

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

## リポジトリ

このスキルは [TBSten/skills](https://github.com/TBSten/skills) の一部です。
