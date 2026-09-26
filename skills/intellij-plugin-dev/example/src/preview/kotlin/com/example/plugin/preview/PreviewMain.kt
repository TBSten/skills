@file:OptIn(InternalComposeUiApi::class) // renderComposeScene

package com.example.plugin.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.renderComposeScene
import com.example.plugin.ui.ExampleModel
import com.example.plugin.ui.ExampleToolWindowContent
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.system.exitProcess

/**
 * Headless preview: renders the Jewel/Compose UI to PNGs without starting an IDE, writes a
 * gallery, and syncs or compares the PNGs with the golden snapshots.
 *
 * Run through Gradle:
 * - `./gradlew updatePreview` — render all PNGs, write build/preview/index.html, and overwrite the
 *   golden (snapshots/preview) with them.
 * - `./gradlew verifyPreview` — render all PNGs and compare them with the golden; exit non-zero on
 *   any difference (changed / new / missing). The report is build/preview/report/index.html.
 *
 * Verify first; update the golden only after a human has approved the difference.
 */

/**
 * One preview PNG per scenario and theme. CUSTOMIZE: replace with the matrix of the real UI,
 * covering edge cases (narrow width, long names, empty state) as well as the happy path.
 */
private data class Scenario(
    val name: String,
    val width: Int,
    val height: Int,
    val model: ExampleModel,
)

private val scenarios = listOf(
    Scenario("default", width = 480, height = 320, model = ExampleModel("Example Plugin", listOf("Alpha", "Beta", "Gamma"))),
    // Narrow width, to catch wrapping and overflow.
    Scenario(
        "narrow", width = 320, height = 320,
        model = ExampleModel("Example Plugin (narrow)", listOf("A very long item name that should wrap or clip", "Beta")),
    ),
)

private val themes = listOf("light" to false, "dark" to true)

fun main(args: Array<String>) {
    // Also passed as jvmArgs by Gradle; set here too so that running main() directly works.
    System.setProperty("java.awt.headless", "true")
    System.setProperty("skiko.renderApi", "SOFTWARE") // software rasterizer, no GPU needed

    val mode = args.firstOrNull()
    if (mode != "update" && mode != "verify") {
        System.err.println("usage: PreviewMainKt <update|verify>  (gradle: updatePreview / verifyPreview)")
        exitProcess(2)
    }

    // Relative to the working directory, which Gradle's JavaExec sets to the project directory.
    val outDir = File("build/preview")
    val goldenDir = File("snapshots/preview") // committed

    // Drop PNGs of renamed or removed scenarios before rendering.
    PreviewChecks.cleanManagedOutputs(outDir)
    outDir.mkdirs()

    val expected = scenarios.flatMap { s -> themes.map { (theme, _) -> "preview-${s.name}-$theme.png" } }.toSet()
    for (scenario in scenarios) {
        for ((theme, dark) in themes) {
            renderScenario(scenario, dark, File(outDir, "preview-${scenario.name}-$theme.png"))
        }
    }
    writeGallery(outDir, expected.sorted())

    // Machine-checked gates, so that a visual review cannot talk itself into a broken image.
    val gateFailures = buildList {
        addAll(PreviewChecks.unexpectedFileSet(outDir, expected))
        addAll(
            PreviewChecks.transparentCornerPngs(expected.sorted().map { File(outDir, it) }).map {
                "transparent corner: ${it.name} — paint the render root with the theme background"
            },
        )
    }
    if (gateFailures.isNotEmpty()) {
        System.err.println("preview gates failed (${gateFailures.size}):")
        gateFailures.forEach { System.err.println("  - $it") }
        exitProcess(1)
    }

    when (mode) {
        "update" -> {
            PreviewChecks.syncGolden(outDir, goldenDir, expected)
            println("golden updated: ${goldenDir.path} (${expected.size} PNGs). Review the diff and commit it.")
            println("gallery: ${File(outDir, "index.html").path}")
        }
        "verify" -> {
            val diff = PreviewChecks.diffAgainstGolden(outDir, goldenDir, expected)
            if (diff.isEmpty()) {
                println("verifyPreview OK: matches the golden (${expected.size} PNGs)")
            } else {
                val report = writeReport(outDir, goldenDir, diff)
                System.err.println("verifyPreview failed: differs from the golden")
                diff.changed.forEach { System.err.println("  changed: $it") }
                diff.new.forEach { System.err.println("  new (not in the golden): $it") }
                diff.missing.forEach { System.err.println("  missing (only in the golden): $it") }
                System.err.println("Open ${report.path} to compare before/after. If the change is intended, run updatePreview after approval.")
                exitProcess(1)
            }
        }
    }
}

/** Standalone Jewel Int UI theme + renderComposeScene → PNG. */
private fun renderScenario(scenario: Scenario, dark: Boolean, out: File) {
    val image = renderComposeScene(width = scenario.width, height = scenario.height) {
        IntUiTheme(isDark = dark) {
            // Paint the whole root with the theme background; the transparent-corner gate relies on it.
            Box(Modifier.fillMaxSize().background(JewelTheme.globalColors.panelBackground)) {
                ExampleToolWindowContent(scenario.model)
            }
        }
    }
    out.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
}

/** One page showing every PNG, for review by an agent or a human. */
private fun writeGallery(outDir: File, names: List<String>) {
    val rows = names.joinToString("\n") { name ->
        """<figure><img src="$name" alt="$name"><figcaption>$name</figcaption></figure>"""
    }
    File(outDir, "index.html").writeText(
        """
        <!doctype html>
        <meta charset="utf-8">
        <title>Example Plugin preview gallery</title>
        <style>
            body { font-family: sans-serif; background: #808080; }
            figure { display: inline-block; margin: 8px; }
            img { display: block; border: 1px solid #333; image-rendering: pixelated; }
            figcaption { font-size: 12px; text-align: center; }
        </style>
        <h1>preview gallery</h1>
        $rows
        """.trimIndent(),
    )
}

/** Before/after report of a failed verify. Copies golden and actual PNGs so that it stands alone. */
private fun writeReport(outDir: File, goldenDir: File, diff: PreviewChecks.GoldenDiff): File {
    val reportDir = File(outDir, "report").apply { mkdirs() }
    File(reportDir, "golden").mkdirs()
    File(reportDir, "actual").mkdirs()

    fun copied(sub: String, dir: File, name: String): String? {
        val src = File(dir, name)
        if (!src.isFile) return null
        src.copyTo(File(reportDir, "$sub/$name"), overwrite = true)
        return "$sub/$name"
    }

    fun row(name: String, status: String): String {
        val golden = copied("golden", goldenDir, name)
        val actual = copied("actual", outDir, name)
        fun cell(path: String?) = path?.let { """<img src="$it" alt="$it">""" } ?: "<em>(none)</em>"
        return """<tr><td>$name</td><td>$status</td><td>${cell(golden)}</td><td>${cell(actual)}</td></tr>"""
    }

    val rows = diff.changed.joinToString("\n") { row(it, "changed") } + "\n" +
        diff.new.joinToString("\n") { row(it, "new") } + "\n" +
        diff.missing.joinToString("\n") { row(it, "missing") }
    val report = File(reportDir, "index.html")
    report.writeText(
        """
        <!doctype html>
        <meta charset="utf-8">
        <title>verifyPreview report</title>
        <style>
            body { font-family: sans-serif; background: #808080; }
            table { border-collapse: collapse; }
            td, th { border: 1px solid #333; padding: 6px; vertical-align: top; }
            img { display: block; max-width: 480px; image-rendering: pixelated; }
        </style>
        <h1>verifyPreview report (golden vs actual)</h1>
        <table>
            <tr><th>png</th><th>status</th><th>golden (before)</th><th>actual (after)</th></tr>
            $rows
        </table>
        """.trimIndent(),
    )
    return report
}
