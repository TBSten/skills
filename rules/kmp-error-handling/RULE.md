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

## 1. AppError — sealed class によるエラーモデル

アプリ全体で使うエラーは `sealed class AppError` で定義する。

- エラーコード体系で分類する（1xxx=ネットワーク, 2xxx=認証, 3xxx=データ, 9xxx=その他）
- `shouldAutoRetry` プロパティで一時的エラー（Network, Timeout, ServerError）を識別する
- `Exception` を継承し、エラーコードとメッセージを `"[$errorCode] $message"` 形式で保持する
- 新しいエラーを追加する際は既存のコード体系に従い、適切なカテゴリに配置する

```kotlin
sealed class AppError(errorCode: String, message: String) : Exception("[$errorCode] $message") {
    open val shouldAutoRetry: Boolean = false

    // 1xxx: ネットワーク系（自動リトライ対象）
    class Network(message: String) : AppError("1001", message) {
        override val shouldAutoRetry: Boolean = true
    }

    // 2xxx: 認証・認可系（リトライ非対象）
    class Unauthorized(message: String) : AppError("2001", message)

    // 3xxx: データ系
    class NotFound(message: String) : AppError("3001", message)

    // 9xxx: その他
    open class Unknown(message: String) : AppError("9999", message)
}
```

## 2. エラーマッピング — Data 層で AppError に変換

Data 層の HTTP クライアント等で発生する例外は、統一的に `AppError` に変換する。

- `Throwable.toAppError()` 拡張関数で `when` 分岐によりマッピングする
- HTTP ステータスコード → 対応する `AppError` サブクラス
- `IOException` → `AppError.Network`
- `SerializationException` → `AppError.DataParse`
- 既に `AppError` の場合はそのまま返す
- 未知の例外は `AppError.Unknown` にフォールバックする

```kotlin
fun Throwable.toAppError(): AppError = when (this) {
    is ResponseException -> when (response.status.value) {
        401 -> AppError.Unauthorized(message)
        404 -> AppError.NotFound(message)
        in 500..599 -> AppError.ServerError(message)
        else -> AppError.Unknown.Api(message)
    }
    is IOException -> AppError.Network(message ?: "Network error")
    is SerializationException -> AppError.DataParse(message ?: "Data parse error")
    is AppError -> this
    else -> AppError.Unknown(message ?: "Unknown error")
}
```

## 3. CancellationException の扱い — 必ず再スロー

suspend 関数内で例外をキャッチする際は、`CancellationException` を **絶対に握りつぶさない**。

- `runCatching` を suspend 関数内で直接使わない。代わりに `runSuspendCatching` を使う
- `runSuspendCatching` は `CancellationException` をキャッチせず再スローする
- retry / recover 等のユーティリティも全て `CancellationException` を即座に伝播させる

```kotlin
suspend inline fun <R> runSuspendCatching(block: suspend () -> R) = try {
    Result.success(block())
} catch (cancel: CancellationException) {
    throw cancel  // 必ず再スロー
} catch (e: Throwable) {
    Result.failure(e)
}
```

## 4. HandleError / HandleWarning — エラー・警告のハンドリング戦略

### HandleError interface

エラーハンドリングのロジックを抽象化する interface。

- `handle(exception)` でエラーを処理する
- `invoke { ... }` で同期ブロックのエラーを自動キャッチ
- `invokeSuspend { ... }` で suspend ブロックのエラーを自動キャッチ（`runSuspendCatching` 使用）
- `plus` 演算子で複数のハンドラを合成可能

```kotlin
interface HandleError {
    fun handle(exception: Throwable)

    operator fun invoke(block: () -> Unit) {
        runCatching { block() }.onFailure { handle(it) }
    }

    suspend fun invokeSuspend(block: suspend () -> Unit) {
        runSuspendCatching { block() }.onFailure { handle(it) }
    }

    operator fun plus(other: HandleError): HandleError = object : HandleError {
        override fun handle(exception: Throwable) {
            this@HandleError.handle(exception)
            other.handle(exception)
        }
    }
}
```

### HandleWarning interface

警告を文字列ベースで通知する interface。エラーとは別に管理する。

```kotlin
interface HandleWarning {
    operator fun invoke(vararg warnings: String)

    operator fun plus(other: HandleWarning): HandleWarning = object : HandleWarning {
        override fun invoke(vararg warnings: String) {
            this@HandleWarning(*warnings)
            other(*warnings)
        }
    }
}
```

### 配置と層の責務

| 層 | ファイル | 責務 |
|---|---|---|
| Domain | `HandleError` interface | エラーハンドリング契約の定義 |
| Domain | `HandleWarning` interface | 警告ハンドリング契約の定義 |
| UI | `HandleErrorDefault` 実装 | `mutableStateListOf` でエラーを保持し UI に通知 |
| Data | `CrashlyticsHandleError` 等 | Crashlytics 等の外部サービスへの報告 |
| App | DI (`ErrorHandlingProviders`) | 実装のバインディング。`plus` で複数ハンドラを合成 |

### ViewModel での利用パターン

ViewModel では `handleError` を DI で受け取り、ユーザー操作ごとに `handleError { ... }` で囲む。

```kotlin
class SomeViewModel(
    private val handleError: HandleError,
) : ViewModel() {
    fun doSomething() = handleError {
        // 例外が発生しても handleError が処理する
        someUseCase()
    }
}
```

### DI 設定パターン

```kotlin
@ContributesTo(AppScope::class)
interface ErrorHandlingProviders {
    @Provides
    @SingleIn(AppScope::class)
    fun provideHandleErrorDefault(): HandleErrorDefault = HandleErrorDefault()

    @Binds
    val HandleErrorDefault.bindsHandleError: HandleError

    @Provides
    fun provideHandleWarning(): HandleWarning = object : HandleWarning {
        override fun invoke(vararg warnings: String) {
            // Debug: println, Release: Crashlytics 等
        }
    }
}
```

### テストでの利用パターン

テスト時は Fake 実装を使い、エラー・警告が正しく処理されたか検証する。

```kotlin
class HandleErrorForTest : HandleError {
    val errors = mutableListOf<Throwable>()
    override fun handle(exception: Throwable) {
        errors.add(exception)
    }
}

class HandleWarningForTest : HandleWarning {
    val warnings = mutableListOf<List<String>>()
    override fun invoke(vararg warnings: String) {
        this.warnings.add(warnings.toList())
    }
}
```

## 5. リトライ・リカバリユーティリティ — 関数型チェーン

Domain 層で `suspend () -> R` の拡張関数としてリトライ・リカバリを提供する。

### 基本ルール

- 全てのユーティリティは `CancellationException` を即座に再スローする
- `suspend () -> R` の拡張関数として実装し、メソッドチェーンで組み合わせ可能にする
- `yield()` でコルーチンに協調的にスケジューラを譲る

### 利用可能なユーティリティ

| 関数 | 用途 |
|---|---|
| `retry(maxRetryCount)` | 最大 N 回リトライ |
| `retryIf(predicate)` | 条件付きリトライ |
| `retryWithBackoff(maxRetryCount, initialDelay, maxDelay, factor)` | 指数バックオフリトライ |
| `recover(transform)` | 例外をフォールバック値に変換 |
| `onSuccess(action)` | 成功時の副作用（ログ等） |
| `onError(action)` | 失敗時の副作用（ログ等） |
| `timeout(duration)` | タイムアウト設定 |
| `measure(onMeasured)` | 実行時間計測 |
| `minimumDelay(duration)` | 最低実行時間保証（UX 用） |

### チェーン例

```kotlin
suspend { api.fetchData() }
    .retryWithBackoff(maxRetryCount = 3)
    .timeout(30.seconds)
    .onError { logger.error(it) }
    .invoke()
```
