# kmp-snapshot-testing ルール

[English](./kmp-snapshot-testing.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Multiplatform プロジェクト向けのスナップショット PBT (Property-Based Testing) ルール。Kotest と Turbine を使用。

## クイックスタート

### 1. ルールをインストール:

```bash
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-snapshot-testing
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

### テスト骨格テンプレート

新規テストは AI にボイラープレートをゼロから書かせるのではなく、`docs/test/templates/` の骨格をコピーして TODO を埋めます:

| テンプレート | 用途 |
|---|---|
| `templates/__Target__PbtSnapshotTest.kt` | StateHolder / ViewModel の PBT スナップショットテスト |
| `templates/__Target__LogicPbtSnapshotTest.kt` | ロジック・関数の PBT スナップショットテスト |
| `templates/__Target__ComposeSnapshotPbt.kt` | Compose UI の PBT スナップショットテスト |

## インストールされるファイル

| ファイル | 説明 |
|---|---|
| `.claude/rules/kmp-snapshot-testing.md` | ルール定義 (パストリガー) |
| `docs/test/README.md` | テスト戦略の概要 |
| `docs/test/snapshot-test.md` | スナップショット PBT テストガイド (状態ホルダー、ロジック) |
| `docs/test/compose-snapshot-test.md` | Compose UI スナップショットテストガイド |
| `docs/test/templates/*.kt` | テスト骨格テンプレート (コピーして TODO を埋める) |

> **注意:** 再インストールするとインストールされるファイルは上書きされます。`docs/test/` 以下へのローカルなカスタマイズも上書きされる点に注意してください。

## 関連

プロジェクトにスナップショットテスト基盤がまだない場合は、`kmp-snapshot-testing-setup` スキルでセットアップできます:

```bash
gh skill install tbsten/skills kmp-snapshot-testing-setup
```
