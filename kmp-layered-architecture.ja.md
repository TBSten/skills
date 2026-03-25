# kmp-layered-architecture ルール

[English](./kmp-layered-architecture.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Multiplatform + Compose プロジェクト向けの 4 層アーキテクチャ (App / UI / Domain / Data) を強制する [Claude Code](https://docs.anthropic.com/en/docs/claude-code) ルール。

## クイックスタート

### 1. ルールをインストール:

```bash
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- kmp-layered-architecture
```

### 2. コーディング開始:

`app/`, `ui/`, `domain/`, `data/` ディレクトリ内のファイルを変更すると、Claude Code が自動的にアーキテクチャドキュメントを読んでから作業します。

## 動作内容

**パストリガールール** です。4 層のいずれかのコードが変更される際、対応するアーキテクチャドキュメントを事前に読むよう Claude Code に指示します。

| パスパターン | ドキュメント |
|---|---|
| `app/**/*.kt` | `docs/architecture/app.md` |
| `ui/**/*.kt` | `docs/architecture/ui.md` |
| `domain/**/*.kt` | `docs/architecture/domain.md` |
| `data/**/*.kt` | `docs/architecture/data.md` |

## インストールされるファイル

| ファイル | 説明 |
|---|---|
| `.claude/rules/kmp-layered-architecture.md` | ルール定義 (パストリガー) |
| `docs/architecture/README.md` | アーキテクチャ概要とレイヤー依存関係図 |
| `docs/architecture/app.md` | App 層: エントリーポイント、DI、フレーバー処理 |
| `docs/architecture/ui.md` | UI 層: 画面、ViewModel、ナビゲーション |
| `docs/architecture/domain.md` | Domain 層: UseCase、Repository (インターフェース) |
| `docs/architecture/data.md` | Data 層: Repository 実装、API クライアント |

## カスタマイズ

インストール後、`docs/architecture/` 内のドキュメントをプロジェクト固有のアーキテクチャ方針、命名規則、DI フレームワークに合わせて編集してください。
