package com.example.feedback

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

actual fun httpTransport(endpoint: String, token: String?): FeedbackTransport = FeedbackTransport { json ->
    if (endpoint.isBlank()) return@FeedbackTransport FeedbackResult(false, error = "受け口の URL が未設定")
    withContext(Dispatchers.IO) {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
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

actual fun encodePng(image: ImageBitmap): ByteArray? = runCatching {
    ByteArrayOutputStream().use { out -> image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray() }
}.getOrNull()

// toImageBitmap() は hardware bitmap で Canvas に描けないので software に写す
actual fun prepareShot(image: ImageBitmap): ImageBitmap = image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false).asImageBitmap()

actual fun platformEnv(): Map<String, Any?> = mapOf(
    "os" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
)
