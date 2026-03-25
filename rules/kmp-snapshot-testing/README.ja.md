# kmp-snapshot-testing ルール

[English](./README.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Multiplatform プロジェクト向けのスナップショット PBT (Property-Based Testing) ルール。Kotest と Turbine を使用。

## クイックスタート

### 1. ルールをインストール:

```bash
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- kmp-snapshot-testing
```

### 2. コーディング開始:

スナップショットテストコードやテスト基盤を変更すると、Claude Code が自動的にテストドキュメントを読んでから作業します。

## 動作内容

**パストリガールール** です。スナップショットテストコードやテスト基盤が変更される際、テストドキュメントを事前に読むよう Claude Code に指示します。

| パスパターン | 説明 |
|---|---|
| `**/jvmSnapshotTest/**/*.kt` | スナップショットテストファイル |
| `core/testing/**/*.kt` | コアテスト基盤 |
| `ui/core/testing/**/*.kt` | UI テスト基盤 |

## インストールされるファイル

| ファイル | 説明 |
|---|---|
| `.claude/rules/kmp-snapshot-testing.md` | ルール定義 (パストリガー) |
| `docs/test/README.md` | テスト戦略の概要 |
| `docs/test/snapshot-test.md` | スナップショット PBT テストガイド (状態ホルダー、ロジック) |
| `docs/test/compose-snapshot-test.md` | Compose UI スナップショットテストガイド |

## 関連

プロジェクトにスナップショットテスト基盤がまだない場合は、`kmp-snapshot-testing-setup` スキルでセットアップできます:

```bash
npx skills add tbsten/skills --skill kmp-snapshot-testing-setup
```
