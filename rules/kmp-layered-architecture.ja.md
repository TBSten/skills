# kmp-layered-architecture ルール

[English](./kmp-layered-architecture.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

Kotlin Multiplatform + Compose プロジェクト向けの 4 層アーキテクチャ (App / UI / Domain / Data) を強制する [Claude Code](https://docs.anthropic.com/en/docs/claude-code) ルール。

## クイックスタート

### 1. ルールをインストール:

```bash
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
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

### 規約の機械検証 & feature scaffold

規約の遵守を AI の記憶に頼らないために、以下を同梱しています:

- **`docs/architecture/templates/ArchitectureConventionTest.kt`** — [Konsist](https://docs.konsist.lemonappdev.com/) による規約テストのテンプレート。層間依存 (`README.md` の依存図どおりか)、`Providers` サフィックス強制 (`Module` / 単数 `Provider` 禁止)、`Impl` / `Fake` 命名を検証します。jvmTest に配置し、冒頭の TODO 定数を差し替えて使います。
- **`tools/kmp-layered-architecture/new-feature.sh`** — `docs/architecture/templates/feature/` から `ui/feature/<name>/` (Screen / ViewModel / Navigation) を生成する scaffold script。既存ファイル (settings.gradle.kts, ui/navigation の各ファイル, DI Providers) への追記は自動化できないため、追記すべきスニペットを stdout に印字します (AI または人間が適用):

```bash
bash tools/kmp-layered-architecture/new-feature.sh Home com.example.app.ui.feature.home
```

## インストールされるファイル

| ファイル | 説明 |
|---|---|
| `.claude/rules/kmp-layered-architecture.md` | ルール定義 (パストリガー) |
| `docs/architecture/README.md` | アーキテクチャ概要とレイヤー依存関係図 |
| `docs/architecture/app.md` | App 層: エントリーポイント、DI、フレーバー処理 |
| `docs/architecture/ui.md` | UI 層: 画面、ViewModel、ナビゲーション |
| `docs/architecture/domain.md` | Domain 層: UseCase、Repository (インターフェース) |
| `docs/architecture/data.md` | Data 層: Repository 実装、API クライアント |
| `docs/architecture/templates/ArchitectureConventionTest.kt` | Konsist 規約テストテンプレート |
| `docs/architecture/templates/feature/*` | feature scaffold テンプレート (Screen / ViewModel / Navigation) |
| `tools/kmp-layered-architecture/new-feature.sh` | feature scaffold script (`bash` で実行) |

## カスタマイズ

インストール後、`docs/architecture/` 内のドキュメントをプロジェクト固有のアーキテクチャ方針、命名規則、DI フレームワークに合わせて編集してください。

> **注意:** 再インストールするとインストールされるファイルは上書きされます。`docs/architecture/` や `tools/kmp-layered-architecture/` 以下へのローカルなカスタマイズも上書きされる点に注意してください。
