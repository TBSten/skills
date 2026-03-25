package com.example.snapshot.testing.snapshot.assertion

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

private val depthColors = listOf(
    Color(66, 133, 244, 60),   // blue
    Color(52, 168, 83, 60),    // green
    Color(251, 188, 4, 60),    // yellow
    Color(234, 67, 53, 60),    // red
    Color(171, 71, 188, 60),   // purple
    Color(0, 172, 193, 60),    // cyan
)

private val depthBorderColors = listOf(
    Color(66, 133, 244),
    Color(52, 168, 83),
    Color(251, 188, 4),
    Color(234, 67, 53),
    Color(171, 71, 188),
    Color(0, 172, 193),
)

private val labelFont = Font("SansSerif", Font.PLAIN, 11)

internal fun renderSemanticsLayout(interaction: SemanticsNodeInteraction): BufferedImage {
    val rootNode = interaction.fetchSemanticsNode()
    val bounds = rootNode.boundsInRoot

    val width = maxOf(bounds.right.roundToInt(), 1)
    val height = maxOf(bounds.bottom.roundToInt(), 1)

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    // 白背景
    g.color = Color.WHITE
    g.fillRect(0, 0, width, height)

    drawNode(g, rootNode, depth = 0)

    g.dispose()
    return image
}

private fun drawNode(g: java.awt.Graphics2D, node: SemanticsNode, depth: Int) {
    val bounds = node.boundsInRoot
    val x = bounds.left.roundToInt()
    val y = bounds.top.roundToInt()
    val w = (bounds.right - bounds.left).roundToInt()
    val h = (bounds.bottom - bounds.top).roundToInt()

    if (w <= 0 || h <= 0) return

    val colorIndex = depth % depthColors.size

    // 半透明の塗り
    g.color = depthColors[colorIndex]
    g.fillRect(x, y, w, h)

    // 枠線
    g.color = depthBorderColors[colorIndex]
    g.stroke = BasicStroke(1f)
    g.drawRect(x, y, w, h)

    // ラベル
    val label = buildNodeLabel(node)
    if (label.isNotEmpty()) {
        g.font = labelFont
        val fm = g.fontMetrics
        val labelX = x + 2
        val labelY = y + fm.ascent + 1

        // ラベル背景
        g.color = Color(255, 255, 255, 200)
        g.fillRect(labelX, y + 1, fm.stringWidth(label) + 4, fm.height)

        // ラベルテキスト
        g.color = depthBorderColors[colorIndex].darker()
        g.drawString(label, labelX + 2, labelY)
    }

    // 子ノードを再帰描画
    for (child in node.children) {
        drawNode(g, child, depth + 1)
    }
}

private fun buildNodeLabel(node: SemanticsNode): String {
    val parts = mutableListOf<String>()

    node.config.getOrNull(SemanticsProperties.Text)
        ?.firstOrNull()
        ?.let { parts.add("\"${it.text}\"") }

    node.config.getOrNull(SemanticsProperties.Role)
        ?.let { parts.add(it.toString()) }

    node.config.getOrNull(SemanticsProperties.ContentDescription)
        ?.firstOrNull()
        ?.let { parts.add("[${it}]") }

    node.config.getOrNull(SemanticsProperties.TestTag)
        ?.let { parts.add("#$it") }

    if (parts.isEmpty()) return ""
    return parts.joinToString(" ")
}
