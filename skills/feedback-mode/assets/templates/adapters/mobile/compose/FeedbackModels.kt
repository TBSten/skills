package com.example.feedback

import androidx.compose.ui.graphics.ImageBitmap

// 差し込み口（references/setup/adapter.md §1）を引数で受ける。プラットフォームの API はここに入れない
class FeedbackConfig(
    val appName: String,
    val transport: FeedbackTransport,
    val pointer: String? = null,
    val getScreen: () -> String = { "" },
    val getView: () -> Map<String, Any?> = { emptyMap() },
    val getEnv: () -> Map<String, Any?> = { emptyMap() },
    val getRecentLogs: () -> List<FeedbackLog> = { emptyList() },
    // null ならスクショを撮らない（O1 を入れない）
    val encodePng: ((ImageBitmap) -> ByteArray?)? = null,
    // 撮った画像を描き直せる形にする。Android は hardware bitmap なので software に写す（android/debug/kotlin/FeedbackHost.kt）
    val prepareShot: (ImageBitmap) -> ImageBitmap = { it },
    // 既存の debug menu から controller.enter() を呼ぶなら false にする
    val showEntryButton: Boolean = true,
)

data class FeedbackLog(val ts: String, val level: String, val text: String)

data class FeedbackResult(val ok: Boolean, val id: String? = null, val ref: String? = null, val error: String? = null)

// 画面 → 受け口。json は feedback.schema.json の送信 body
fun interface FeedbackTransport {
    suspend fun send(json: String): FeedbackResult
}

// 受け口の返事 {"ok":true,"id":"…","ref":"…"} / {"ok":false,"error":"…"} を読む（依存を増やさないため正規表現）
fun parseFeedbackResponse(status: Int, body: String): FeedbackResult {
    fun str(key: String) = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(body)?.groupValues?.get(1)
    val ok = status in 200..299 && Regex("\"ok\"\\s*:\\s*true").containsMatchIn(body)
    return if (ok) FeedbackResult(true, str("id"), str("ref"))
    else FeedbackResult(false, error = str("error") ?: "HTTP $status")
}
