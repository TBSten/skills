package com.example.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// entry: 既存の debug menu からはこれの enter() を呼ぶ
class FeedbackController {
    var active by mutableStateOf(false)
        private set
    fun enter() { active = true }
    fun exit() { active = false }

    companion object {
        // 回転・テーマの切り替えでモードが終わらないように（rememberSaveable で使う）
        val Saver: Saver<FeedbackController, Boolean> = Saver(save = { it.active }, restore = { FeedbackController().apply { if (it) enter() } })
    }
}

// アプリの根を包む。content はスクショ用に GraphicsLayer へ記録し、モードの UI はその外（上）に重ねる
// backHandler: Android は { enabled, onBack -> BackHandler(enabled, onBack) } を渡す（戻るをアプリに届かせない: R3）
@Composable
fun FeedbackRoot(
    config: FeedbackConfig,
    controller: FeedbackController = rememberSaveable(saver = FeedbackController.Saver) { FeedbackController() },
    backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> },
    content: @Composable () -> Unit,
) {
    val registry = remember { FbRegistry() }
    val layer = rememberGraphicsLayer()
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val focus = LocalFocusManager.current
    Box(Modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInWindow(); rootSize = it.size }) {
        CompositionLocalProvider(LocalFbRegistry provides registry) {
            Box(Modifier.fillMaxSize().drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }) { content() }
        }
        backHandler(controller.active) { controller.exit() }
        if (controller.active) {
            FeedbackOverlay(config, registry, rootOffset, rootSize, onExit = { controller.exit() }) { layer.toImageBitmap() }
        } else if (config.showEntryButton) {
            Chip("FB", Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(12.dp)) {
                focus.clearFocus() // R8: 入力中の欄を確定させずに外す
                controller.enter()
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun FeedbackOverlay(
    config: FeedbackConfig,
    registry: FbRegistry,
    rootOffset: Offset,
    rootSize: IntSize,
    onExit: () -> Unit,
    snapshot: suspend () -> androidx.compose.ui.graphics.ImageBitmap,
) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<FbEntry>?>(null) } // null = まだ選んでいない、空 = 画面全体
    var index by remember { mutableIntStateOf(0) }
    var tap by remember { mutableStateOf(Offset.Zero) }
    var comment by rememberSaveable { mutableStateOf("") } // 回転しても書きかけを残す
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selected = candidates?.getOrNull(index)
    // 選んだ部品の矩形（このオーバーレイ = アプリの根からの px）
    val local = selected?.bounds?.translate(-rootOffset)

    Box(
        Modifier.fillMaxSize()
            // block: 最前面で全部のポインタを受けて consume する。下のアプリにも、祖先の drag（ModalBottomSheet など）にも届かない（R3）
            .pointerInput(registry) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // 子（シートのボタン・余白）がすでに使ったタップは、選び直しに使わない
                    var byChild = down.isConsumed
                    down.consume()
                    var last = down
                    var moved = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.isConsumed }) byChild = true
                        ev.changes.forEach { it.consume() }
                        val c = ev.changes.first()
                        if ((c.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                        last = c
                        if (ev.changes.none { it.pressed }) break
                    }
                    if (!moved && !byChild && !sending) {
                        tap = last.position
                        candidates = registry.candidatesAt(last.position + rootOffset)
                        index = 0
                        error = null
                    }
                }
            }
            .drawBehind {
                if (local != null) {
                    drawRect(Color(0x33FF0000), local.topLeft, local.size)
                    drawRect(Color.Red, local.topLeft, local.size, style = Stroke(2.dp.toPx()))
                }
            },
    ) {
        Row(
            Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(8.dp)
                .background(Color(0xE6222222)).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("フィードバック: 部品をタップ", style = TextStyle(color = Color.White, fontSize = 14.sp))
            Chip("閉じる", onClick = onExit)
        }
        if (candidates != null) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)) // ジェスチャナビの帯とキーボードに重ねない
                    .pointerInput(Unit) { detectTapGestures { } } // シートの余白のタップで選び直さない
                    .background(Color.White).border(1.dp, Color(0xFFCCCCCC)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicText(selected?.name ?: "（画面全体）", style = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)))
                BasicTextField(
                    comment, { comment = it },
                    Modifier.fillMaxWidth().heightIn(min = 72.dp).background(Color(0xFFF2F2F2)).padding(8.dp),
                    textStyle = TextStyle(fontSize = 16.sp),
                    decorationBox = { inner ->
                        Box {
                            if (comment.isEmpty()) BasicText("この部品へのコメント（空でも送れる）", style = TextStyle(fontSize = 16.sp, color = Color(0xFF888888)))
                            inner()
                        }
                    },
                )
                error?.let { BasicText(it, style = TextStyle(color = Color(0xFFCC0000), fontSize = 13.sp)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(if (sending) "送信中…" else "送る") {
                        if (sending) return@Chip
                        sending = true
                        error = null
                        scope.launch {
                            val result = runCatching {
                                val shots = config.encodePng?.let { encode ->
                                    runCatching { renderShots(config.prepareShot(snapshot()), local, density) }
                                        .map { Triple(encode(it.window), it.element?.let(encode), null as String?) }
                                        .getOrElse { e -> Triple(null, null, "撮れませんでした: ${e.message}") }
                                }
                                val body = mapOf(
                                    "payload" to mapOf(
                                        "appName" to config.appName,
                                        "pointer" to config.pointer,
                                        "kind" to if (selected != null) "element" else "page",
                                        "comment" to comment,
                                        "target" to mapOf(
                                            "screen" to config.getScreen(),
                                            "path" to (selected?.name ?: config.getScreen()),
                                            "rect" to local?.let { dpRect(it, density) },
                                            "click" to mapOf("x" to tap.x / density, "y" to tap.y / density),
                                        ),
                                        "viewport" to mapOf("width" to rootSize.width / density, "height" to rootSize.height / density, "dpr" to density),
                                        "env" to config.getEnv(),
                                        "view" to config.getView(),
                                        "logs" to config.getRecentLogs().map { mapOf("ts" to it.ts, "level" to it.level, "text" to it.text) },
                                    ),
                                    "options" to mapOf("target" to true, "log" to true, "shots" to (config.encodePng != null), "env" to true, "screen" to false, "view" to true),
                                    "windowPng" to shots?.first?.let { Base64.encode(it) },
                                    "elementPng" to shots?.second?.let { Base64.encode(it) },
                                    "shotError" to (shots?.third ?: if (config.encodePng == null) "スクショは無効" else null),
                                )
                                config.transport.send(toJson(body))
                            }.getOrElse { FeedbackResult(false, error = it.message ?: it.toString()) }
                            sending = false
                            if (result.ok) onExit() else error = "送れませんでした: ${result.error}" // R6
                        }
                    }
                    if (index + 1 < (candidates?.size ?: 0)) Chip("親を選ぶ") { index++ }
                    Chip("閉じる") { candidates = null; error = null } // 書いた文は残す（次に選んだ部品へ持っていく）
                }
            }
        }
    }
}

private fun dpRect(r: Rect, density: Float) =
    mapOf("x" to r.left / density, "y" to r.top / density, "width" to r.width / density, "height" to r.height / density)

@Composable
private fun Chip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    BasicText(
        label,
        modifier.background(Color(0xFF1F6FEB)).clickable(onClick = onClick).heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        style = TextStyle(color = Color.White, fontSize = 14.sp),
    )
}
