package com.example.feedback

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// O1: 全体（選んだ部品に赤枠）と周り 24dp の切り抜き。倍率は min(density, 2)
internal class Shots(val window: ImageBitmap, val element: ImageBitmap?)

// src: モードの UI を含まないアプリの画像（px）。rect: 選んだ部品（src と同じ座標の px）。null なら枠も切り抜きも無し
internal fun renderShots(src: ImageBitmap, rect: Rect?, density: Float): Shots {
    val scale = min(density, 2f) / density
    val stroke = 3f * density
    fun frame(canvas: Canvas, r: Rect, dx: Float, dy: Float) {
        val paint = Paint().apply { color = Color.Red; style = PaintingStyle.Stroke; strokeWidth = stroke * scale }
        canvas.drawRect(Rect((r.left - dx) * scale, (r.top - dy) * scale, (r.right - dx) * scale, (r.bottom - dy) * scale), paint)
    }
    val w = max(1, (src.width * scale).roundToInt())
    val h = max(1, (src.height * scale).roundToInt())
    val window = ImageBitmap(w, h)
    Canvas(window).also { c ->
        c.drawImageRect(src, IntOffset.Zero, IntSize(src.width, src.height), IntOffset.Zero, IntSize(w, h), Paint())
        if (rect != null) frame(c, rect, 0f, 0f)
    }
    val element = rect?.let { r ->
        val pad = 24f * density
        val left = max(0f, r.left - pad).toInt()
        val top = max(0f, r.top - pad).toInt()
        val right = min(src.width.toFloat(), r.right + pad).toInt()
        val bottom = min(src.height.toFloat(), r.bottom + pad).toInt()
        if (right <= left || bottom <= top) return@let null
        val cw = max(1, ((right - left) * scale).roundToInt())
        val ch = max(1, ((bottom - top) * scale).roundToInt())
        ImageBitmap(cw, ch).also { img ->
            val c = Canvas(img)
            c.drawImageRect(src, IntOffset(left, top), IntSize(right - left, bottom - top), IntOffset.Zero, IntSize(cw, ch), Paint())
            frame(c, r, left.toFloat(), top.toFloat())
        }
    }
    return Shots(window, element)
}
