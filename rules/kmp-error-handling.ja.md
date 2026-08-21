# kmp-error-handling ルール

[English](./kmp-error-handling.md)

Kotlin Multiplatform + Compose プロジェクト向けのエラーハンドリング・ワーニング検知 [Claude Code](https://docs.anthropic.com/en/docs/claude-code) ルール。

## クイックスタート

### 1. ルールをインストール:

```bash
curl -fsSL \
  https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | \
  bash -s -- kmp-error-handling
```

### 2. コーディング開始:

エラーハンドリング関連ファイル（`domain/**/error/`, `domain/**/util/`, `data/`, `core/**/*Catching*`, `app/**/ErrorHandling*`, `ui/**/error/`）を変更する際、Claude Code が自動的にエラーハンドリング規約を適用します。

## 機能

**パストリガー型ルール**で、4 層 KMP アーキテクチャ全体のエラーハンドリングパターンを統一します。

### 主要な規約

1. **AppError sealed class** — エラーコード体系（1xxx=ネットワーク, 2xxx=認証, 3xxx=データ, 9xxx=その他）と `shouldAutoRetry` フラグによるエラー分類
2. **エラーマッピング** — Data 層で HTTP/IO/シリアライズ例外を `toAppError()` で `AppError` に統一変換
3. **CancellationException の扱い** — 必ず再スロー。suspend 関数内では `runCatching` の代わりに `runSuspendCatching` を使用
4. **HandleError / HandleWarning** — Domain 層で interface 定義、UI/Data 層で実装、App 層で DI 合成するエラー・警告ハンドラ
5. **リトライ・リカバリユーティリティ** — `suspend () -> R` の拡張関数として retry, backoff, recover, timeout, measure を関数型チェーンで提供

### 層ごとの責務

| 層 | 責務 |
|---|---|
| **Domain** | `AppError`, `HandleError`, `HandleWarning` interface 定義、retry/recover ユーティリティ提供 |
| **Data** | 生の例外を `AppError` にマッピング、`CrashlyticsHandleError` 等の実装 |
| **UI** | `HandleErrorDefault`（Observable なエラーリスト）の実装、ViewModel での `handleError { }` パターン |
| **App** | DI バインディング設定、`plus` 演算子で複数ハンドラを合成 |

### コピーして使える完全実装

`docs/error-handling/examples/` に規約に沿った完全実装を同梱しています。AI に毎回実装させるのではなく、必要なファイルをコピーして package を置換して使ってください（各ファイル先頭の TODO コメントに配置先の説明があります）:

| ファイル | 内容 | 配置先の層 |
|---|---|---|
| `AppError.kt` | エラーモデル（sealed class、全サブクラス） | Domain |
| `ToAppError.kt` | 例外 → AppError マッピング | Data |
| `RunSuspendCatching.kt` | suspend 関数用 runCatching 代替 | Domain / Core |
| `SuspendFunctionChain.kt` | retry 等 9 ユーティリティの完全実装 | Domain |
| `HandleError.kt` / `HandleWarning.kt` | ハンドラ interface | Domain |
| `HandleErrorDefault.kt` | UI 向けデフォルト実装（Observable なエラーリスト） | UI |
| `ErrorHandlingProviders.kt` | DI バインディング（Metro 形式、DI に合わせて読み替え） | App |
| `HandleErrorForTest.kt` | テスト用 Fake（HandleErrorForTest / HandleWarningForTest） | テストコード |

## インストールされるファイル

| ファイル | 説明 |
|---|---|
| `.claude/rules/kmp-error-handling.md` | ルール定義（パストリガー型） |
| `docs/error-handling/examples/*.kt` | 全規約のコピーして使える完全実装 |

## カスタマイズ

インストール後、エラーコード体系やリトライ戦略をプロジェクトの要件に合わせて調整してください。

> **注意:** 再インストールするとインストールされるファイルは上書きされます。`docs/error-handling/` 以下へのローカルなカスタマイズも上書きされる点に注意してください。
