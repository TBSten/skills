package com.example.plugin

import com.example.plugin.preview.PreviewChecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * preview の純出力ゲートを test から叩く (references/setup/snapshot.md「test から純出力ゲートを叩く」)。
 * PreviewChecks は純 JVM (Compose 非依存) なので、`testImplementation(sourceSets["preview"].output)`
 * だけで standalone Compose を classpath に載せずにテストできる。
 */
class PreviewOutputGateTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun writePng(dir: File, name: String, transparentCorner: Boolean): File {
        val img = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color(0x2B, 0x2D, 0x30)
        g.fillRect(0, 0, 8, 8)
        g.dispose()
        if (transparentCorner) img.setRGB(0, 0, 0x00000000)
        val file = File(dir, name)
        ImageIO.write(img, "png", file)
        return file
    }

    @Test
    fun `透明角のある PNG だけを検出する`() {
        val dir = tmp.newFolder()
        val opaque = writePng(dir, "preview-opaque-light.png", transparentCorner = false)
        val transparent = writePng(dir, "preview-transparent-dark.png", transparentCorner = true)

        val detected = PreviewChecks.transparentCornerPngs(listOf(opaque, transparent))

        assertEquals(listOf(transparent), detected)
    }

    @Test
    fun `expected filename set との不一致を両方向で検出する`() {
        val dir = tmp.newFolder()
        writePng(dir, "preview-default-light.png", transparentCorner = false)
        writePng(dir, "preview-stale-light.png", transparentCorner = false)

        val problems = PreviewChecks.unexpectedFileSet(
            dir,
            expected = setOf("preview-default-light.png", "preview-default-dark.png"),
        )

        assertEquals(2, problems.size)
        assertTrue(problems.any { it.contains("preview-stale-light.png") })   // 期待に無い
        assertTrue(problems.any { it.contains("preview-default-dark.png") })  // 生成されなかった
    }

    @Test
    fun `golden との差分を changed と new と missing に分類する`() {
        val outDir = tmp.newFolder("out")
        val goldenDir = tmp.newFolder("golden")
        // same: 両方に同一内容 / changed: 内容が違う / new: golden に無い / missing: golden にだけある
        writePng(outDir, "preview-same-light.png", transparentCorner = false)
        File(goldenDir, "preview-same-light.png").writeBytes(File(outDir, "preview-same-light.png").readBytes())
        writePng(outDir, "preview-changed-light.png", transparentCorner = false)
        writePng(goldenDir, "preview-changed-light.png", transparentCorner = true)
        writePng(outDir, "preview-new-light.png", transparentCorner = false)
        writePng(goldenDir, "preview-missing-light.png", transparentCorner = false)

        val diff = PreviewChecks.diffAgainstGolden(
            outDir,
            goldenDir,
            expected = setOf("preview-same-light.png", "preview-changed-light.png", "preview-new-light.png"),
        )

        assertEquals(listOf("preview-changed-light.png"), diff.changed)
        assertEquals(listOf("preview-new-light.png"), diff.new)
        assertEquals(listOf("preview-missing-light.png"), diff.missing)
        assertTrue(!diff.isEmpty())
    }
}
