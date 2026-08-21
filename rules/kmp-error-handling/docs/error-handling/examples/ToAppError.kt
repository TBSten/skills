// 配置先: Data 層 (例: data/api/ToAppError.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

// TODO: 使用している HTTP クライアント・IO ライブラリに合わせて import を調整する。
//  以下は Ktor 3 (kotlinx-io) + kotlinx.serialization の例。
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Data 層で発生する生の例外を [AppError] に統一変換する。
 *
 * - HTTP ステータスコード → 対応する [AppError] サブクラス
 * - タイムアウト → [AppError.Timeout]
 * - [IOException] → [AppError.Network]
 * - [SerializationException] → [AppError.DataParse]
 * - 既に [AppError] の場合はそのまま返す
 * - 未知の例外は [AppError.Unknown] にフォールバックする
 *
 * CancellationException はこの関数に到達させない
 * (キャッチ側で runSuspendCatching を使い、先に再スローさせる)。
 */
fun Throwable.toAppError(): AppError = when (this) {
    // HttpRequestTimeoutException は IOException のサブクラスのため、Network より先に分岐する
    is HttpRequestTimeoutException -> AppError.Timeout(message ?: "Request timeout")
    is ResponseException -> when (response.status.value) {
        401 -> AppError.Unauthorized(message ?: "Unauthorized")
        404 -> AppError.NotFound(message ?: "Not found")
        in 500..599 -> AppError.ServerError(message ?: "Server error")
        else -> AppError.Unknown.Api(message ?: "Unexpected API error")
    }
    is IOException -> AppError.Network(message ?: "Network error")
    is SerializationException -> AppError.DataParse(message ?: "Data parse error")
    is AppError -> this
    else -> AppError.Unknown(message ?: "Unknown error")
}
