package com.example.feedback

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

// 名前を付けた部品の矩形（px・ウィンドウ座標）を覚えておく。タップのときだけ読むので state にしない
class FbRegistry {
    private val entries = LinkedHashMap<Any, FbEntry>()
    internal fun put(key: Any, entry: FbEntry) { entries[key] = entry }
    internal fun remove(key: Any) { entries.remove(key) }

    // タップ位置を含む部品を小さい順に。先頭が選ぶ部品、後ろが「親を選ぶ」の候補
    fun candidatesAt(p: Offset): List<FbEntry> =
        entries.values.filter { it.bounds.contains(p) }.sortedBy { it.bounds.width * it.bounds.height }
}

data class FbEntry(val name: String, val bounds: Rect)

val LocalFbRegistry = staticCompositionLocalOf<FbRegistry?> { null }

// 部品の名前（R5）。name は外側からつないだ道筋（"ホーム/保存ボタン"）をそのまま書く
// release では同じ名前の no-op（android/release/kotlin/FeedbackHost.kt）か、KMP なら hook 経由で外す
fun Modifier.fbTarget(name: String): Modifier = composed {
    val registry = LocalFbRegistry.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(registry, key) { onDispose { registry.remove(key) } }
    onGloballyPositioned { registry.put(key, FbEntry(name, it.boundsInWindow())) }
}
