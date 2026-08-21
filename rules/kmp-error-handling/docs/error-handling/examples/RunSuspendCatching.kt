// 配置先: Domain 層または Core (例: core/coroutines/RunSuspendCatching.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

import kotlin.coroutines.cancellation.CancellationException

/**
 * suspend 関数内で使う [runCatching] の代替。
 *
 * 標準の [runCatching] は [CancellationException] まで握りつぶしてしまい、
 * コルーチンのキャンセルが伝播しなくなる。suspend 関数内では必ずこちらを使うこと。
 */
suspend inline fun <R> runSuspendCatching(block: suspend () -> R): Result<R> = try {
    Result.success(block())
} catch (cancel: CancellationException) {
    throw cancel // 必ず再スロー
} catch (e: Throwable) {
    Result.failure(e)
}
