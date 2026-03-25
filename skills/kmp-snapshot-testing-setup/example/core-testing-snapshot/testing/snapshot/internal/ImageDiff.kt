package com.example.snapshot.testing.snapshot.internal

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal val DIFF_PIXEL_COLOR = Color.RED.rgb
internal val MATCH_PIXEL_COLOR = Color(128, 128, 128, 80).rgb

internal fun saveActualAndDiffImages(
    actual: BufferedImage,
    actualPixels: IntArray,
    expectedPixels: IntArray,
    width: Int,
    height: Int,
    actualFile: File,
    diffFile: File,
) {
    ImageIO.write(actual, "png", actualFile)

    val diffImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val diffPixels = IntArray(actualPixels.size) { i ->
        if (actualPixels[i] != expectedPixels[i]) DIFF_PIXEL_COLOR else MATCH_PIXEL_COLOR
    }
    diffImage.setRGB(0, 0, width, height, diffPixels, 0, width)
    ImageIO.write(diffImage, "png", diffFile)
}

internal fun buildImageDiffClue(
    diffCount: Int,
    totalPixels: Int,
    diffRate: Double,
    threshold: Double,
    expectedFile: File,
    actualFile: File,
    diffFile: File,
    sizeMismatch: Boolean = false,
    actualSize: String = "",
    expectedSize: String = "",
): String = buildString {
    appendLine(
        "Image differs: $diffCount/$totalPixels pixels (%.2f%%), threshold: %.2f%%"
            .format(diffRate * 100, threshold * 100),
    )
    if (sizeMismatch) {
        appendLine("  size: actual=$actualSize, expected=$expectedSize")
    }
    appendLine("  expected: ${expectedFile.absolutePath}")
    appendLine("  actual:   ${actualFile.absolutePath}")
    appendLine("  diff:     ${diffFile.absolutePath}")
}
