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

## インストールされるファイル

| ファイル | 説明 |
|---|---|
| `.claude/rules/kmp-error-handling.md` | ルール定義（パストリガー型） |

## カスタマイズ

インストール後、エラーコード体系やリトライ戦略をプロジェクトの要件に合わせて調整してください。
