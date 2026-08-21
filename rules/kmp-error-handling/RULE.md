---
paths:
  - "domain/**/error/**/*.kt"
  - "domain/**/util/**/*.kt"
  - "data/**/*.kt"
  - "core/**/*Catching*.kt"
  - "app/**/ErrorHandling*.kt"
  - "ui/**/error/**/*.kt"
---

エラーハンドリング・ワーニング検知に関連するコードを変更する際は、以下の規約に従うこと。

**実装をゼロから書かないこと。** 規約に沿った完全実装が `docs/error-handling/examples/` にある。
必要になったら該当ファイルをコピーし、ファイル先頭の TODO コメントに従って package・配置先を
プロジェクトに合わせて置換して使う。

| ファイル (docs/error-handling/examples/) | 内容 | 配置先の層 |
|---|---|---|
| `AppError.kt` | エラーモデル (sealed class) | Domain |
| `ToAppError.kt` | 例外 → AppError マッピング | Data |
| `RunSuspendCatching.kt` | suspend 用 runCatching 代替 | Domain / Core |
| `SuspendFunctionChain.kt` | retry 等 9 ユーティリティ | Domain |
| `HandleError.kt` / `HandleWarning.kt` | ハンドラ interface | Domain |
| `HandleErrorDefault.kt` | UI 向けデフォルト実装 | UI |
| `ErrorHandlingProviders.kt` | DI バインディング | App |
| `HandleErrorForTest.kt` | テスト用 Fake (Error / Warning) | テストコード |

## 1. AppError — sealed class によるエラーモデル

- アプリ全体で使うエラーは `sealed class AppError` で定義する
- エラーコード体系で分類する（1xxx=ネットワーク・通信, 2xxx=認証, 3xxx=データ, 9xxx=その他）
- `shouldAutoRetry` プロパティで一時的エラー（Network, Timeout, ServerError）を識別する
- `Exception` を継承し、エラーコードとメッセージを `"[$errorCode] $message"` 形式で保持する
- 新しいエラーを追加する際は既存のコード体系に従い、適切なカテゴリに配置する

## 2. エラーマッピング — Data 層で AppError に変換

- Data 層の HTTP クライアント等で発生する例外は `Throwable.toAppError()` 拡張関数で
  `when` 分岐により統一的に `AppError` に変換する
- HTTP ステータスコード → 対応する `AppError` サブクラス
  （401=Unauthorized, 404=NotFound, 5xx=ServerError, その他=Unknown.Api）
- リクエストタイムアウト → `AppError.Timeout` / `IOException` → `AppError.Network` /
  `SerializationException` → `AppError.DataParse`
- 既に `AppError` の場合はそのまま返し、未知の例外は `AppError.Unknown` にフォールバックする

## 3. CancellationException の扱い — 必ず再スロー

- suspend 関数内で例外をキャッチする際は、`CancellationException` を **絶対に握りつぶさない**
- `runCatching` を suspend 関数内で直接使わない。代わりに `runSuspendCatching` を使う
  （`CancellationException` をキャッチせず再スローする）
- retry / recover 等のユーティリティも全て `CancellationException` を即座に伝播させる

## 4. HandleError / HandleWarning — エラー・警告のハンドリング戦略

- `HandleError`: エラーハンドリングを抽象化する interface。`handle(exception)` で処理、
  `invoke { ... }` で同期ブロック、`invokeSuspend { ... }` で suspend ブロック
  （`runSuspendCatching` 使用）のエラーを自動キャッチ、`plus` 演算子で複数ハンドラを合成できる
- `HandleWarning`: 警告を文字列ベースで通知する interface。エラーとは別に管理する
- 層の責務: Domain=interface 定義 / UI=`HandleErrorDefault`（`mutableStateListOf` でエラーを
  保持し UI に通知）/ Data=`CrashlyticsHandleError` 等の外部サービス報告 /
  App=DI（`ErrorHandlingProviders`）でバインディングし `plus` で合成
- ViewModel では `handleError` を DI で受け取り、ユーザー操作ごとに `handleError { ... }` で囲む
- テストでは Fake 実装（`HandleErrorForTest` / `HandleWarningForTest`）を使い、
  エラー・警告が正しく処理されたか検証する

## 5. リトライ・リカバリユーティリティ — 関数型チェーン

- Domain 層で `suspend () -> R` の拡張関数として提供し、メソッドチェーンで組み合わせる
- 全てのユーティリティは `CancellationException` を即座に再スローし、
  リトライ前に `yield()` でコルーチンに協調的にスケジューラを譲る
- 提供するユーティリティ（実装は `examples/SuspendFunctionChain.kt`）:
  `retry(maxRetryCount)` / `retryIf(predicate)` /
  `retryWithBackoff(maxRetryCount, initialDelay, maxDelay, factor)` / `recover(transform)` /
  `onSuccess(action)` / `onError(action)` / `timeout(duration)` / `measure(onMeasured)` /
  `minimumDelay(duration)`

```kotlin
suspend { api.fetchData() }
    .retryWithBackoff(maxRetryCount = 3)
    .timeout(30.seconds)
    .onError { logger.error(it) }
    .invoke()
```
