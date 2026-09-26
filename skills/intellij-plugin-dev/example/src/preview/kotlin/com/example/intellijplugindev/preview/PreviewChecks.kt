package com.example.intellijplugindev.preview

import java.io.File
import javax.imageio.ImageIO

/**
 * Automatic gates over the preview output.
 *
 * Kept pure JVM so that `test` can call it through `testImplementation(sourceSets["preview"].output)`:
 * do not import Compose here, or standalone Compose would be loaded next to the IDE's bundled one.
 */
object PreviewChecks {

    /** Files this harness owns. Anything else is left alone by clean and golden sync. */
    private val managedPng = Regex("""preview-.*\.png""")

    fun isManagedPng(name: String): Boolean = managedPng.matches(name)

    /**
     * Deletes the owned outputs (preview-*.png / index.html / report/) before rendering, so that
     * PNGs of renamed or removed scenarios do not linger in the gallery or the golden.
     */
    fun cleanManagedOutputs(dir: File) {
        dir.listFiles()?.forEach { f ->
            if (f.isFile && (isManagedPng(f.name) || f.name == "index.html")) f.delete()
        }
        File(dir, "report").deleteRecursively()
    }

    /** Checks that the rendered PNGs are exactly [expected]. Returns readable problems (empty = OK). */
    fun unexpectedFileSet(dir: File, expected: Set<String>): List<String> {
        val actual = dir.listFiles()
            ?.filter { it.isFile && isManagedPng(it.name) }
            ?.map { it.name }?.toSet().orEmpty()
        return buildList {
            (actual - expected).sorted().forEach { add("unexpected PNG rendered: $it") }
            (expected - actual).sorted().forEach { add("expected PNG not rendered: $it") }
        }
    }

    /**
     * Returns the PNGs whose corner pixels are not fully opaque. When the render root does not paint
     * the theme surface the background is transparent, and dark markers, thin lines and table
     * headers vanish in a dark viewer. Give intentionally transparent PNGs another suffix.
     */
    fun transparentCornerPngs(pngs: List<File>): List<File> = pngs.filter { file ->
        if (!file.isFile) return@filter false // missing files are reported by unexpectedFileSet
        val img = ImageIO.read(file) ?: return@filter true // an undecodable file fails too
        val xs = intArrayOf(0, img.width - 1)
        val ys = intArrayOf(0, img.height - 1)
        xs.any { x -> ys.any { y -> (img.getRGB(x, y) ushr 24) != 0xFF } }
    }

    /** Result of verify. changed = bytes differ / new = not in the golden / missing = only in the golden. */
    data class GoldenDiff(
        val changed: List<String>,
        val new: List<String>,
        val missing: List<String>,
    ) {
        fun isEmpty(): Boolean = changed.isEmpty() && new.isEmpty() && missing.isEmpty()
    }

    /**
     * verify: compares the rendered PNGs with the golden (snapshots/preview) byte by byte, relying on
     * rendering being deterministic on one machine. Does not modify the golden.
     */
    fun diffAgainstGolden(outDir: File, goldenDir: File, expected: Set<String>): GoldenDiff {
        val goldenNames = goldenDir.listFiles()
            ?.filter { it.isFile && isManagedPng(it.name) }
            ?.map { it.name }?.toSet().orEmpty()
        val changed = expected.filter { name ->
            val golden = File(goldenDir, name)
            val actual = File(outDir, name)
            golden.isFile && actual.isFile && !golden.readBytes().contentEquals(actual.readBytes())
        }.sorted()
        return GoldenDiff(
            changed = changed,
            new = (expected - goldenNames).sorted(),
            missing = (goldenNames - expected).sorted(),
        )
    }

    /** update: makes the golden equal to the rendered PNGs, deleting stale ones. Keeps non-owned files (.gitkeep etc.). */
    fun syncGolden(outDir: File, goldenDir: File, expected: Set<String>) {
        goldenDir.mkdirs()
        goldenDir.listFiles()?.forEach { f ->
            if (f.isFile && isManagedPng(f.name) && f.name !in expected) f.delete()
        }
        expected.sorted().forEach { name ->
            File(outDir, name).copyTo(File(goldenDir, name), overwrite = true)
        }
    }
}
