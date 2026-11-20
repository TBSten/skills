package com.example.feedback

import androidx.compose.ui.graphics.ImageBitmap

// :feedback モジュールの commonMain に、compose/ の 5 ファイルと一緒に置く。実装は各ターゲットの PlatformFeedback.<target>.kt
// transport: 1 件ごとに HTTP POST（Android エミュ 10.0.2.2:7373・iOS シミュ / Desktop / Web は 127.0.0.1:7373）
expect fun httpTransport(endpoint: String, token: String? = null): FeedbackTransport
expect fun encodePng(image: ImageBitmap): ByteArray?
expect fun prepareShot(image: ImageBitmap): ImageBitmap
expect fun platformEnv(): Map<String, Any?>

// 各ターゲットの値を入れた FeedbackConfig。差し込み口（getScreen など）は呼ぶ側で足す
fun platformFeedbackConfig(
    appName: String,
    endpoint: String,
    token: String? = null,
    pointer: String? = null,
    getScreen: () -> String = { "" },
    getView: () -> Map<String, Any?> = { emptyMap() },
    getRecentLogs: () -> List<FeedbackLog> = { emptyList() },
) = FeedbackConfig(
    appName = appName,
    transport = httpTransport(endpoint, token),
    pointer = pointer,
    getScreen = getScreen,
    getView = getView,
    getEnv = ::platformEnv,
    getRecentLogs = getRecentLogs,
    encodePng = ::encodePng,
    prepareShot = ::prepareShot,
)
