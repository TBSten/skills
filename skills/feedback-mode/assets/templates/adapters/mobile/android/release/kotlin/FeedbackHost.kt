package com.example.feedback

// src/release/kotlin/… に置く no-op。debug の FeedbackHost / fbTarget と同じ形にし、main からはこの 2 つだけを呼ぶ
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class FeedbackLog(val ts: String, val level: String, val text: String)

@Composable
fun FeedbackHost(
    appName: String,
    getScreen: () -> String = { "" },
    getView: () -> Map<String, Any?> = { emptyMap() },
    getRecentLogs: () -> List<FeedbackLog> = { emptyList() },
    content: @Composable () -> Unit,
) = content()

@Suppress("NOTHING_TO_INLINE")
inline fun Modifier.fbTarget(@Suppress("UNUSED_PARAMETER") name: String): Modifier = this
