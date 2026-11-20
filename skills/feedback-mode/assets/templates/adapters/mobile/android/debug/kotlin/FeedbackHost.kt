package com.example.feedback

// src/debug/kotlin/… に置く。src/release/kotlin/… には同じ名前の no-op（release/FeedbackHost.kt）を置く
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.example.fbdemo.BuildConfig // TODO: 導入先の namespace の BuildConfig に変える（BuildConfig の package は namespace で決まる）

@Composable
fun FeedbackHost(
    appName: String,
    getScreen: () -> String = { "" },
    getView: () -> Map<String, Any?> = { emptyMap() },
    getRecentLogs: () -> List<FeedbackLog> = { emptyList() },
    // 既存の debug menu から入るなら showEntryButton = false にし、同じ controller の enter() をその項目から呼ぶ
    showEntryButton: Boolean = true,
    controller: FeedbackController = rememberSaveable(saver = FeedbackController.Saver) { FeedbackController() },
    content: @Composable () -> Unit,
) {
    val config = remember {
        FeedbackConfig(
            appName = appName,
            transport = httpTransport(BuildConfig.FEEDBACK_ENDPOINT, BuildConfig.FEEDBACK_TOKEN.ifBlank { null }),
            pointer = null, // 例: "ソースは app/src/main/kotlin、ログの形は README の『ログ』"
            getScreen = getScreen,
            getView = getView,
            getEnv = ::androidEnv,
            getRecentLogs = getRecentLogs,
            encodePng = ::encodePng,
            prepareShot = ::toSoftwareBitmap,
            showEntryButton = showEntryButton,
        )
    }
    FeedbackRoot(config, controller = controller, backHandler = { enabled, onBack -> BackHandler(enabled, onBack) }, content = content)
}

// transport: 1 件ごとに HTTP POST（エミュレータは http://10.0.2.2:7373/__feedback）。token は受け口を LAN に出すときだけ（x-feedback-token）
fun httpTransport(endpoint: String, token: String? = null) = FeedbackTransport { json ->
    if (endpoint.isBlank()) return@FeedbackTransport FeedbackResult(false, error = "受け口の URL が未設定（gradle property feedback.endpoint）")
    withContext(Dispatchers.IO) {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 5_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json; charset=utf-8")
            token?.let { conn.setRequestProperty("x-feedback-token", it) }
            conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            val body = (if (status < 400) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            parseFeedbackResponse(status, body)
        } finally {
            conn.disconnect()
        }
    }
}

// GraphicsLayer.toImageBitmap() は hardware bitmap で、Canvas に描けない（"Software rendering doesn't support hardware bitmaps"）
private fun toSoftwareBitmap(image: ImageBitmap): ImageBitmap =
    image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false).asImageBitmap()

private fun encodePng(image: ImageBitmap): ByteArray? = runCatching {
    ByteArrayOutputStream().use { out -> image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray() }
}.getOrNull()

private fun androidEnv(): Map<String, Any?> = mapOf(
    "os" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
    "build" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE}",
)
