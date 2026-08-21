// 配置先: Domain 層 (例: domain/core/error/AppError.kt)
// TODO: package をプロジェクトに合わせて置換する
package com.example.errorhandling

/**
 * アプリ全体で使うエラーモデル。
 *
 * - エラーコード体系で分類する (1xxx=ネットワーク・通信, 2xxx=認証・認可, 3xxx=データ, 9xxx=その他)
 * - [shouldAutoRetry] が true のエラー (Network / Timeout / ServerError) は
 *   一時的エラーとして自動リトライの対象になる
 * - 新しいエラーを追加する際は既存のコード体系に従い、適切なカテゴリに配置する
 */
sealed class AppError(errorCode: String, message: String) : Exception("[$errorCode] $message") {
    open val shouldAutoRetry: Boolean = false

    // 1xxx: ネットワーク・通信系（自動リトライ対象）

    /** 接続不可・切断などのネットワークエラー。 */
    class Network(message: String) : AppError("1001", message) {
        override val shouldAutoRetry: Boolean = true
    }

    /** 通信タイムアウト。 */
    class Timeout(message: String) : AppError("1002", message) {
        override val shouldAutoRetry: Boolean = true
    }

    /** サーバー側エラー (HTTP 5xx)。一時的な障害の可能性が高いためリトライ対象。 */
    class ServerError(message: String) : AppError("1003", message) {
        override val shouldAutoRetry: Boolean = true
    }

    // 2xxx: 認証・認可系（リトライ非対象）

    /** 未認証・認可エラー (HTTP 401 等)。 */
    class Unauthorized(message: String) : AppError("2001", message)

    // 3xxx: データ系

    /** データが見つからない (HTTP 404 等)。 */
    class NotFound(message: String) : AppError("3001", message)

    /** レスポンス等のパース失敗 (SerializationException 等)。 */
    class DataParse(message: String) : AppError("3002", message)

    // 9xxx: その他

    /** 分類できない未知のエラー。マッピングのフォールバック先。 */
    open class Unknown(message: String, errorCode: String = "9999") : AppError(errorCode, message) {
        /** API が想定外のエラーレスポンス (ハンドリング対象外のステータスコード等) を返した場合。 */
        class Api(message: String) : Unknown(message, errorCode = "9001")
    }
}
