// 配置先: Domain 層 (例: domain/core/util/SuspendFunctionChain.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

/**
 * `suspend () -> R` の拡張関数として提供するリトライ・リカバリユーティリティ。
 *
 * 共通ルール:
 * - 全て新しい `suspend () -> R` を返すため、メソッドチェーンで組み合わせられる
 * - 全て [CancellationException] を即座に再スローする (絶対に握りつぶさない)
 * - リトライ系は再試行の前に [yield] / [delay] でコルーチンに協調的にスケジューラを譲る
 *
 * 使用例:
 * ```
 * suspend { api.fetchData() }
 *     .retryWithBackoff(maxRetryCount = 3)
 *     .timeout(30.seconds)
 *     .onError { logger.error(it) }
 *     .invoke()
 * ```
 */

/**
 * 失敗時に最大 [maxRetryCount] 回リトライする (初回実行を含め最大 [maxRetryCount] + 1 回実行)。
 */
fun <R> (suspend () -> R).retry(
    maxRetryCount: Int = 3,
): suspend () -> R = retryIf(maxRetryCount = maxRetryCount) { true }

/**
 * [predicate] が true を返す例外の場合のみ、最大 [maxRetryCount] 回リトライする。
 *
 * 例: `.retryIf { it is AppError && it.shouldAutoRetry }`
 */
fun <R> (suspend () -> R).retryIf(
    maxRetryCount: Int = 3,
    predicate: (Throwable) -> Boolean,
): suspend () -> R {
    val block = this
    return {
        var attempt = 0
        var result: Result<R>
        while (true) {
            result = try {
                Result.success(block())
            } catch (cancel: CancellationException) {
                throw cancel // キャンセルはリトライせず即座に伝播
            } catch (e: Throwable) {
                Result.failure(e)
            }
            val error = result.exceptionOrNull() ?: break
            if (attempt >= maxRetryCount || !predicate(error)) break
            attempt++
            yield() // 再試行の前にスケジューラへ協調的に譲る
        }
        result.getOrThrow()
    }
}

/**
 * 指数バックオフ付きで最大 [maxRetryCount] 回リトライする。
 * 待機時間は [initialDelay] から [factor] 倍ずつ増え、[maxDelay] を上限とする。
 */
fun <R> (suspend () -> R).retryWithBackoff(
    maxRetryCount: Int = 3,
    initialDelay: Duration = 500.milliseconds,
    maxDelay: Duration = 10.seconds,
    factor: Double = 2.0,
): suspend () -> R {
    val block = this
    return {
        var attempt = 0
        var currentDelay = initialDelay
        var result: Result<R>
        while (true) {
            result = try {
                Result.success(block())
            } catch (cancel: CancellationException) {
                throw cancel // キャンセルはリトライせず即座に伝播
            } catch (e: Throwable) {
                Result.failure(e)
            }
            if (result.isSuccess || attempt >= maxRetryCount) break
            attempt++
            delay(currentDelay) // delay 自体が協調的な suspension point
            currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
        }
        result.getOrThrow()
    }
}

/**
 * 失敗時に [transform] でフォールバック値へ復帰する。
 * 特定条件のみ復帰したい場合は [transform] 内で条件に合わない例外を再スローする。
 */
fun <R> (suspend () -> R).recover(
    transform: suspend (Throwable) -> R,
): suspend () -> R {
    val block = this
    return {
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel // キャンセルはリカバリ対象にしない
        } catch (e: Throwable) {
            transform(e)
        }
    }
}

/** 成功時に [action] を実行する (ログ等の副作用用)。結果はそのまま返す。 */
fun <R> (suspend () -> R).onSuccess(
    action: suspend (R) -> Unit,
): suspend () -> R {
    val block = this
    return {
        val result = block()
        action(result)
        result
    }
}

/** 失敗時に [action] を実行する (ログ等の副作用用)。例外はそのまま再スローする。 */
fun <R> (suspend () -> R).onError(
    action: suspend (Throwable) -> Unit,
): suspend () -> R {
    val block = this
    return {
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel // キャンセルはエラーとして扱わない
        } catch (e: Throwable) {
            action(e)
            throw e
        }
    }
}

/**
 * [duration] 以内に完了しない場合は `TimeoutCancellationException` を投げる。
 *
 * 注意: `TimeoutCancellationException` は [CancellationException] のサブクラスのため、
 * このチェーンの後段の [recover] / [onError] では捕捉されない。
 * タイムアウトをハンドリングしたい場合はチェーンの外で [AppError.Timeout] 等へ変換する。
 */
fun <R> (suspend () -> R).timeout(
    duration: Duration,
): suspend () -> R {
    val block = this
    return {
        withTimeout(duration) { block() }
    }
}

/** 成功時に実行時間と結果を [onMeasured] へ渡す (計測ログ等の副作用用)。 */
fun <R> (suspend () -> R).measure(
    onMeasured: suspend (Duration, R) -> Unit,
): suspend () -> R {
    val block = this
    return {
        val (result, duration) = measureTimedValue { block() }
        onMeasured(duration, result)
        result
    }
}

/**
 * 実行完了まで最低 [duration] かかることを保証する
 * (ローディング表示が一瞬で消えるのを防ぐ等の UX 用)。
 * 成功・失敗どちらでも待つが、キャンセル時は待たずに即座に伝播する。
 */
fun <R> (suspend () -> R).minimumDelay(
    duration: Duration,
): suspend () -> R {
    val block = this
    return {
        val mark = TimeSource.Monotonic.markNow()
        val result = try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel // キャンセル時は最低時間を待たない
        } catch (e: Throwable) {
            delay(duration - mark.elapsedNow()) // 経過済みなら負値になり即座に return する
            throw e
        }
        delay(duration - mark.elapsedNow())
        result
    }
}
