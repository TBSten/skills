// 配置先: Domain 層 (例: domain/core/error/HandleError.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

/**
 * エラーハンドリングのロジックを抽象化する interface。
 *
 * - 実装は層ごとに用意する (UI: [HandleErrorDefault], Data: CrashlyticsHandleError 等)
 * - App 層の DI で実装をバインドし、[plus] で複数ハンドラを合成する
 * - ViewModel では DI で受け取り、ユーザー操作ごとに `handleError { ... }` で囲む
 */
interface HandleError {
    /** エラーを処理する。 */
    fun handle(exception: Throwable)

    /** 同期ブロックのエラーを自動キャッチする。 */
    operator fun invoke(block: () -> Unit) {
        runCatching { block() }.onFailure { handle(it) }
    }

    /** suspend ブロックのエラーを自動キャッチする ([runSuspendCatching] 使用)。 */
    suspend fun invokeSuspend(block: suspend () -> Unit) {
        runSuspendCatching { block() }.onFailure { handle(it) }
    }

    /** 複数のハンドラを合成する。合成後は両方の [handle] が順に呼ばれる。 */
    operator fun plus(other: HandleError): HandleError = object : HandleError {
        override fun handle(exception: Throwable) {
            this@HandleError.handle(exception)
            other.handle(exception)
        }
    }
}
