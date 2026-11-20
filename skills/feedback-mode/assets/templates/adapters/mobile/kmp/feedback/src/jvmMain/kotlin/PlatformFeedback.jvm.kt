package com.example.feedback

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.net.HttpURLConnection
import java.net.URI

actual fun httpTransport(endpoint: String, token: String?): FeedbackTransport = FeedbackTransport { json ->
    if (endpoint.isBlank()) return@FeedbackTransport FeedbackResult(false, error = "受け口の URL が未設定")
    withContext(Dispatchers.IO) {
        val conn = URI(endpoint).toURL().openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"; conn.connectTimeout = 5_000; conn.readTimeout = 15_000; conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json; charset=utf-8")
            token?.let { conn.setRequestProperty("x-feedback-token", it) }
            conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            parseFeedbackResponse(status, (if (status < 400) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: "")
        } finally { conn.disconnect() }
    }
}

actual fun encodePng(image: ImageBitmap): ByteArray? =
    runCatching { Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes }.getOrNull()

actual fun prepareShot(image: ImageBitmap): ImageBitmap = image

actual fun platformEnv(): Map<String, Any?> = mapOf(
    "os" to "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})",
    "runtime" to "JVM ${System.getProperty("java.version")}",
)
