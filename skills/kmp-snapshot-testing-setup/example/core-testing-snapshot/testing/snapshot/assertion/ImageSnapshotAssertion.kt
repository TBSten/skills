package com.example.snapshot.testing.snapshot.assertion

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import io.kotest.assertions.withClue
import io.kotest.core.test.TestScope
import io.kotest.matchers.shouldBe
import com.example.snapshot.testing.snapshot.internal.buildImageDiffClue
import com.example.snapshot.testing.snapshot.internal.saveActualAndDiffImages
import com.example.snapshot.testing.snapshot.shouldMatchSnapshot
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun TestScope.shouldMatchSnapshot(
    fileName: String,
    image: ImageBitmap,
    threshold: Double = 0.0,
) = shouldMatchSnapshot(
    fileName = fileName,
    image = image.toAwtImage(),
    threshold = threshold,
)

/**
 * @param threshold 許容する差分ピクセルの割合 (0.0〜1.0)。0.0 = 完全一致、0.01 = 1%まで許容。
 */
fun TestScope.shouldMatchSnapshot(
    fileName: String,
    image: BufferedImage,
    threshold: Double = 0.0,
) = shouldMatchSnapshot(
    fileName = fileName,
    extension = ".png",
    record = {
        ImageIO.write(image, "png", outputStream)
    },
    verify = {
        val expected = ImageIO.read(inputStream)
            ?: error("Failed to read expected snapshot image: $fileName")

        // 大きい方に合わせたキャンバスで左上揃えに比較
        val compareWidth = maxOf(image.width, expected.width)
        val compareHeight = maxOf(image.height, expected.height)
        val totalPixels = compareWidth * compareHeight

        val actualPixels = IntArray(totalPixels)
        val expectedPixels = IntArray(totalPixels)
        image.getRGB(0, 0, image.width, image.height, actualPixels, 0, compareWidth)
        expected.getRGB(0, 0, expected.width, expected.height, expectedPixels, 0, compareWidth)

        val diffCount = actualPixels.indices.count { actualPixels[it] != expectedPixels[it] }
        val diffRate = diffCount.toDouble() / totalPixels

        if (diffRate > threshold) {
            saveActualAndDiffImages(
                actual = image,
                actualPixels = actualPixels,
                expectedPixels = expectedPixels,
                width = compareWidth,
                height = compareHeight,
                actualFile = actualFile,
                diffFile = diffFile,
            )
            withClue(
                buildImageDiffClue(
                    diffCount = diffCount,
                    totalPixels = totalPixels,
                    diffRate = diffRate,
                    threshold = threshold,
                    expectedFile = expectedFile,
                    actualFile = actualFile,
                    diffFile = diffFile,
                    sizeMismatch = image.width != expected.width || image.height != expected.height,
                    actualSize = "${image.width}x${image.height}",
                    expectedSize = "${expected.width}x${expected.height}",
                ),
            ) {
                (diffRate <= threshold) shouldBe true
            }
        }
    },
)
