@file:OptIn(InternalComposeUiApi::class) // renderComposeScene (references/headless-preview.md 中核レシピ)

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
 * headless preview harness (references/headless-preview.md の参照実装 / SSoT)。
 * Jewel/Compose の UI を IDE を起動せず PNG に焼き、gallery を書き、VRT golden と同期/比較する。
 *
 * 起動は gradle の `updatePreview` / `verifyPreview` (references/setup/preview.md で JavaExec 配線):
 * - `update` — 全 PNG を焼く → gallery を書く → golden (snapshots/preview) を強制同期
 * - `verify` — 全 PNG を焼いて golden と比較。差分 (changed/new/missing) があれば非ゼロ終了。
 *   report は build/preview/report/index.html
 *
 * 日々の回し方 (まず verify、golden 更新は人間承認の後だけ) は headless-preview.md「推奨ワークフロー」。
 */

/** 1 枚の preview PNG になるシナリオ。CUSTOMIZE: 実 UI の matrix に置き換える。
 * 正常系だけでなく degraded/edge (narrow 幅・長い名前・空状態など) を網羅する (headless-preview.md)。 */
private data class Scenario(
    val name: String,
    val width: Int,
    val height: Int,
    val model: ExampleModel,
)

private val scenarios = listOf(
    Scenario("default", width = 480, height = 320, model = ExampleModel("Example", listOf("Alpha", "Beta", "Gamma"))),
    // narrow 幅 (折返し・はみ出しの検知用)
    Scenario(
        "narrow", width = 320, height = 320,
        model = ExampleModel("Example (narrow)", listOf("A very long item name that should wrap or clip", "Beta")),
    ),
)

private val themes = listOf("light" to false, "dark" to true)

fun main(args: Array<String>) {
    // gradle の jvmArgs でも渡しているが、単体起動 (IDE の Run 等) でも成立するよう二重に設定する
    System.setProperty("java.awt.headless", "true")
    System.setProperty("skiko.renderApi", "SOFTWARE") // IDE 不要・SW ラスタライズ

    val mode = args.firstOrNull()
    if (mode != "update" && mode != "verify") {
        System.err.println("usage: PreviewMainKt <update|verify>  (gradle: updatePreview / verifyPreview)")
        exitProcess(2)
    }

    // JavaExec の working dir = プラグインモジュールのプロジェクトディレクトリ前提
    val outDir = File("build/preview")
    val goldenDir = File("snapshots/preview") // コミット対象の golden (setup/snapshot.md)

    // managed 出力の事前 clean (rename/削除した旧 scenario の残骸を掃除 — setup/snapshot.md)
    PreviewChecks.cleanManagedOutputs(outDir)
    outDir.mkdirs()

    val expected = scenarios.flatMap { s -> themes.map { (theme, _) -> "preview-${s.name}-$theme.png" } }.toSet()
    for (scenario in scenarios) {
        for ((theme, dark) in themes) {
            renderScenario(scenario, dark, File(outDir, "preview-${scenario.name}-$theme.png"))
        }
    }
    writeGallery(outDir, expected.sorted())

    // 自動ゲート (目視の自己弁護を排す — headless-preview.md / setup/snapshot.md)
    val gateFailures = buildList {
        addAll(PreviewChecks.unexpectedFileSet(outDir, expected))
        addAll(
            PreviewChecks.transparentCornerPngs(expected.sorted().map { File(outDir, it) }).map {
                "透明角 PNG: ${it.name} — render root を theme surface で塗る (headless-preview.md)"
            },
        )
    }
    if (gateFailures.isNotEmpty()) {
        System.err.println("preview 自動ゲート失敗 (${gateFailures.size} 件):")
        gateFailures.forEach { System.err.println("  - $it") }
        exitProcess(1)
    }

    when (mode) {
        "update" -> {
            PreviewChecks.syncGolden(outDir, goldenDir, expected)
            println("golden を更新した: ${goldenDir.path} (${expected.size} PNGs)。差分を確認して commit する")
            println("gallery: ${File(outDir, "index.html").path}")
        }
        "verify" -> {
            val diff = PreviewChecks.diffAgainstGolden(outDir, goldenDir, expected)
            if (diff.isEmpty()) {
                println("verifyPreview OK: golden と一致 (${expected.size} PNGs)")
            } else {
                val report = writeReport(outDir, goldenDir, diff)
                System.err.println("verifyPreview 失敗: golden との差分あり")
                diff.changed.forEach { System.err.println("  changed: $it") }
                diff.new.forEach { System.err.println("  new (golden 未登録): $it") }
                diff.missing.forEach { System.err.println("  missing (golden にだけある): $it") }
                System.err.println("before/after の目視は ${report.path} を開く。意図した変更なら人間承認後に updatePreview")
                exitProcess(1)
            }
        }
    }
}

/** 中核レシピ (headless-preview.md): standalone Jewel Int UI theme + renderComposeScene → PNG */
private fun renderScenario(scenario: Scenario, dark: Boolean, out: File) {
    val image = renderComposeScene(width = scenario.width, height = scenario.height) {
        IntUiTheme(isDark = dark) { // standalone Jewel Int UI → テーマ忠実
            // render root を theme の panel background で全面塗装する (透明角の自動検査を通すため)
            Box(Modifier.fillMaxSize().background(JewelTheme.globalColors.panelBackground)) {
                ExampleToolWindowContent(scenario.model)
            }
        }
    }
    out.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
}

/** 全 PNG を 1 ページで目視できる gallery を書く (エージェント/人間の自己目視用) */
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

/** verify 失敗時の before/after report。golden/actual を report 配下へコピーして自己完結にする */
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
