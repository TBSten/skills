package com.example.plugin.preview

import java.io.File
import javax.imageio.ImageIO

/**
 * preview 出力の純粋な自動ゲート (references/setup/snapshot.md「自動ゲート」)。
 * Compose に依存しない純 JVM 実装に保つ — `testImplementation(sourceSets["preview"].output)` で
 * test からも叩ける (standalone Compose の二重ロードを避けるため、ここに Compose を import しない)。
 */
object PreviewChecks {

    /** managed な生成物のパターン。これ以外は clean / golden 同期で触らない */
    private val managedPng = Regex("""preview-.*\.png""")

    fun isManagedPng(name: String): Boolean = managedPng.matches(name)

    /**
     * 生成前に管理下の生成物 (preview-*.png / index.html / report/) を消す。
     * rename/削除した旧 scenario の PNG が gallery / golden に残り続けるのを防ぐ。
     */
    fun cleanManagedOutputs(dir: File) {
        dir.listFiles()?.forEach { f ->
            if (f.isFile && (isManagedPng(f.name) || f.name == "index.html")) f.delete()
        }
        File(dir, "report").deleteRecursively()
    }

    /** expected filename set との完全一致検査。問題を人間可読メッセージで返す (空 = OK) */
    fun unexpectedFileSet(dir: File, expected: Set<String>): List<String> {
        val actual = dir.listFiles()
            ?.filter { it.isFile && isManagedPng(it.name) }
            ?.map { it.name }?.toSet().orEmpty()
        return buildList {
            (actual - expected).sorted().forEach { add("期待に無い PNG が生成された: $it") }
            (expected - actual).sorted().forEach { add("期待した PNG が生成されなかった: $it") }
        }
    }

    /**
     * 四隅 pixel を含む alpha=255 検査 (references/headless-preview.md「自動ゲート」)。
     * render root が theme surface を塗らないと透明背景 PNG になり、暗い viewer で
     * 黒 marker / 薄線 / table header が消える。透明版が要るときだけ別 suffix にして除外する。
     */
    fun transparentCornerPngs(pngs: List<File>): List<File> = pngs.filter { file ->
        if (!file.isFile) return@filter false // 存在しない分は unexpectedFileSet 側で検出する
        val img = ImageIO.read(file) ?: return@filter true // デコード不能も fail 扱い
        val xs = intArrayOf(0, img.width - 1)
        val ys = intArrayOf(0, img.height - 1)
        xs.any { x -> ys.any { y -> (img.getRGB(x, y) ushr 24) != 0xFF } }
    }

    /** verify の結果。changed = バイト不一致 / new = golden 未登録 / missing = golden にだけある */
    data class GoldenDiff(
        val changed: List<String>,
        val new: List<String>,
        val missing: List<String>,
    ) {
        fun isEmpty(): Boolean = changed.isEmpty() && new.isEmpty() && missing.isEmpty()
    }

    /**
     * verify: 焼いた PNG を golden (snapshots/preview) と比較する。golden は変更しない。
     * 同一マシンで描画がバイト決定的なことを利用したバイト比較 (references/setup/snapshot.md)。
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

    /** update: golden を強制同期する (stale golden の削除も行う)。managed PNG 以外 (.gitkeep 等) は残す */
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
