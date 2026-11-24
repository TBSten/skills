@file:OptIn(ExperimentalWasmJsInterop::class)

package com.example.feedback

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response

// 受け口は Origin が localhost 系だけを許す（CORS の preflight にも答える）
actual fun httpTransport(endpoint: String, token: String?): FeedbackTransport = FeedbackTransport { json ->
    if (endpoint.isBlank()) return@FeedbackTransport FeedbackResult(false, error = "受け口の URL が未設定")
    // 受け口に届かないと fetch は TypeError で落ちる。読める文にして返す（R6: 吹き出しに出してモードを続ける）
    runCatching {
        val res = window.fetch(endpoint, postInit(json, token)).await<Response>()
        parseFeedbackResponse(res.status.toInt(), res.text().await<JsString>().toString())
    }.getOrElse { FeedbackResult(false, error = "受け口に届きません（$endpoint）") }
}

// Kotlin の RequestInit(...) は使わない: 渡していない項目も null で入り、fetch が「'cache' に null は不正」で失敗する
private fun postInit(body: String, token: String?): RequestInit =
    js("({ method: 'POST', body: body, headers: token == null ? { 'content-type': 'application/json' } : { 'content-type': 'application/json', 'x-feedback-token': token } })")

actual fun encodePng(image: ImageBitmap): ByteArray? =
    runCatching { Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes }.getOrNull()

actual fun prepareShot(image: ImageBitmap): ImageBitmap = image

actual fun platformEnv(): Map<String, Any?> = mapOf("userAgent" to window.navigator.userAgent, "url" to window.location.href)
