import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

internal abstract class SnapshotReportTask : DefaultTask() {
    @get:Input
    abstract val snapshotDirPath: Property<String>

    @get:Input
    abstract val baselineManifestPath: Property<String>

    @get:Input
    abstract val beforeRef: Property<String>

    @get:Input
    abstract val afterRef: Property<String>

    @get:Input
    @get:Optional
    abstract val pbtIteration: Property<String>

    companion object {
        private val RESULT_FILE_NAMES = setOf(
            "result.json", "result.md", "result.html",
            "result.sample.json", "result.sample.md",
            "record-report.md", "record-report.html",
            "verify-report.md", "verify-report.html",
        )
        private val TEXT_EXTENSIONS = setOf(
            "kt", "txt", "json", "xml", "md", "html", "css", "js", "ts",
            "yaml", "yml", "properties", "gradle", "kts",
        )
        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif", "svg")
        private const val MAX_DETAILED_ENTRIES = 20
        private const val MAX_COMPACT_ENTRIES = 80
    }

    private fun isArtifactFile(name: String): Boolean =
        name.contains(".actual.") || name.endsWith(".actual") ||
                name.contains(".diff.") || name.endsWith(".diff") ||
                name.contains(".removed.") || name.endsWith(".removed") ||
                name in RESULT_FILE_NAMES

    private fun isTextFile(name: String): Boolean =
        name.substringAfterLast('.', "") in TEXT_EXTENSIONS

    private fun isImageFile(name: String): Boolean =
        name.substringAfterLast('.', "") in IMAGE_EXTENSIONS

    @TaskAction
    fun generate() {
        val snapshotDir = File(snapshotDirPath.get())

        val baselineFiles = parseBaselineManifest(File(baselineManifestPath.get()), snapshotDir)
        val currentFiles = findCurrentFiles(snapshotDir)

        val addFiles = findAddFiles(snapshotDir)
        val removeFiles = (baselineFiles - currentFiles).sorted()
        val updateFiles = findUpdateFiles(snapshotDir)
        val allFiles = baselineFiles.sorted()

        val resultJson = File(snapshotDir, "result.json")
        resultJson.writeText(buildResultJson(allFiles, addFiles, removeFiles, updateFiles))

        val resultMd = File(snapshotDir, "result.md")
        resultMd.writeText(buildResultMd(snapshotDir, allFiles, addFiles, removeFiles, updateFiles))

        val resultHtml = File(snapshotDir, "result.html")
        resultHtml.writeText(buildResultHtml(snapshotDir, allFiles, addFiles, removeFiles, updateFiles))

        logger.lifecycle("Report generated:")
        logger.lifecycle("  - ${resultJson.absolutePath}")
        logger.lifecycle("  - ${resultMd.absolutePath}")
        logger.lifecycle("  - ${resultHtml.absolutePath}")
    }

    private fun parseBaselineManifest(manifest: File, snapshotDir: File): Set<String> {
        if (!manifest.exists()) return emptySet()
        val prefix = snapshotDir.absolutePath + "/"
        return manifest.readText()
            .split('\u0000')
            .filter { it.isNotBlank() }
            .map { it.removePrefix(prefix) }
            .toSet()
    }

    private fun findCurrentFiles(snapshotDir: File): Set<String> =
        snapshotDir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith(".") && !isArtifactFile(it.name) }
            .map { it.relativeTo(snapshotDir).path }
            .toSet()

    private fun findUpdateFiles(snapshotDir: File): List<String> =
        snapshotDir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith(".") && it.name.contains(".actual.") }
            .mapNotNull { actualFile ->
                val rel = actualFile.relativeTo(snapshotDir).path
                val filename = actualFile.name
                val namePart = filename.substringBefore(".actual.")
                val extPart = filename.substringAfter(".actual.")
                val expectedFilename = "$namePart.expected.$extPart"
                val expectedRel = rel.replace(filename, expectedFilename)
                if (File(snapshotDir, expectedRel).exists()) expectedRel else null
            }
            .sorted()
            .toList()

    private fun findAddFiles(snapshotDir: File): List<String> =
        snapshotDir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith(".") && it.name.contains(".actual.") }
            .mapNotNull { actualFile ->
                val rel = actualFile.relativeTo(snapshotDir).path
                val filename = actualFile.name
                val namePart = filename.substringBefore(".actual.")
                val extPart = filename.substringAfter(".actual.")
                val expectedFilename = "$namePart.expected.$extPart"
                val expectedRel = rel.replace(filename, expectedFilename)
                if (!File(snapshotDir, expectedRel).exists()) rel else null
            }
            .sorted()
            .toList()

    private fun actualPathFor(snapshotDir: File, expectedRel: String): String? {
        val actualRel = expectedRel.replace(".expected.", ".actual.")
        if (actualRel != expectedRel && File(snapshotDir, actualRel).exists()) return actualRel
        return null
    }

    // --- JSON generation ---

    private fun buildResultJson(
        allFiles: List<String>,
        addFiles: List<String>,
        removeFiles: List<String>,
        updateFiles: List<String>,
    ): String = buildString {
        val pbt = pbtIteration.orNull
        appendLine("{")
        appendLine("""  "inputs": {""")
        appendLine("""    "-before": ${jsonString(beforeRef.get())},""")
        appendLine("""    "-after": ${jsonString(afterRef.get())},""")
        appendLine("""    "-pbt-iteration": ${pbt ?: "null"}""")
        appendLine("  },")
        appendJsonSection("all", allFiles)
        appendLine(",")
        appendJsonSection("add", addFiles)
        appendLine(",")
        appendJsonSection("remove", removeFiles)
        appendLine(",")
        appendJsonSection("update", updateFiles)
        appendLine()
        appendLine("}")
    }

    private fun StringBuilder.appendJsonSection(name: String, files: List<String>) {
        append("""  "$name": {""")
        appendLine()
        append("""    "count": ${files.size},""")
        appendLine()
        if (files.isEmpty()) {
            append("""    "files": []""")
        } else {
            appendLine("""    "files": [""")
            files.forEachIndexed { i, f ->
                append("      ${jsonString(f)}")
                if (i < files.lastIndex) append(",")
                appendLine()
            }
            append("    ]")
        }
        appendLine()
        append("  }")
    }

    private fun jsonString(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    // --- Markdown generation ---

    private fun buildResultMd(
        snapshotDir: File,
        allFiles: List<String>,
        addFiles: List<String>,
        removeFiles: List<String>,
        updateFiles: List<String>,
    ): String = buildString {
        val totalChanges = updateFiles.size + addFiles.size + removeFiles.size
        val reproduceCmd = buildString {
            append("./tools/snapshot-diff.sh")
            append(" \\\n  -before=${beforeRef.get()}")
            val after = afterRef.get()
            if (after != "current working tree") append(" \\\n  -after=$after")
            pbtIteration.orNull?.let { append(" \\\n  -pbt-iteration=$it") }
        }

        appendLine("## Snapshot diff report")
        appendLine()
        appendLine("<table>")
        appendLine("<tr><th>Summary</th><th>再現方法</th></tr>")
        appendLine("<tr></tr>")
        appendLine("<tr>")
        appendLine("<td>")
        appendLine()
        appendLine("|                 | Count |")
        appendLine("|-----------------|-------|")
        appendLine("| Total snapshots | ${allFiles.size.toString().padEnd(5)} |")
        appendLine("| 🟦 Update       | ${updateFiles.size.toString().padEnd(5)} |")
        appendLine("| 🟢 Add          | ${addFiles.size.toString().padEnd(5)} |")
        appendLine("| ❌ Remove       | ${removeFiles.size.toString().padEnd(5)} |")
        appendLine()
        appendLine("</td>")
        appendLine("<td>")
        appendLine()
        appendLine("```shell")
        appendLine(reproduceCmd)
        appendLine("```")
        appendLine()
        appendLine("</td>")
        appendLine("</tr>")
        appendLine("</table>")
        appendLine()
        if (totalChanges == 0) {
            appendLine("### Snapshots")
            appendLine()
            appendLine("(✅ No snapshot change!)")
        } else {
            appendLine("### Snapshots ($totalChanges)")
            appendLine()
            val entries = buildChangeEntries(updateFiles, addFiles, removeFiles)
            val grouped = groupByTopDir(entries)
            var renderedCount = 0
            for ((dir, dirEntries) in grouped) {
                appendLine("<details>")
                appendLine("<summary><b>$dir</b> (${formatDirSummary(dirEntries)})</summary>")
                appendLine()
                var dirSkipped = 0
                for (entry in dirEntries) {
                    if (renderedCount < MAX_DETAILED_ENTRIES) {
                        when (entry.kind) {
                            ChangeKind.UPDATE -> appendUpdateEntry(snapshotDir, entry.rel)
                            ChangeKind.ADD -> appendAddEntry(snapshotDir, entry.rel)
                            ChangeKind.REMOVE -> appendRemoveEntry(snapshotDir, entry.rel)
                        }
                    } else if (renderedCount < MAX_DETAILED_ENTRIES + MAX_COMPACT_ENTRIES) {
                        val icon = when (entry.kind) {
                            ChangeKind.UPDATE -> "\uD83D\uDFE6"
                            ChangeKind.ADD -> "\uD83D\uDFE2"
                            ChangeKind.REMOVE -> "❌"
                        }
                        appendLine("- $icon `${entry.rel}`")
                    } else {
                        dirSkipped++
                    }
                    renderedCount++
                }
                if (dirSkipped > 0) {
                    appendLine()
                    appendLine("> ... 他 $dirSkipped 件は省略（[snapshot artifact] を参照）")
                }
                appendLine("</details>")
                appendLine()
            }
            if (totalChanges > MAX_DETAILED_ENTRIES) {
                appendLine("> **Note:** 詳細表示は先頭 $MAX_DETAILED_ENTRIES 件のみです。全件は [snapshot artifact] を参照してください。")
                appendLine()
            }
        }
    }

    private data class ChangeEntry(val rel: String, val kind: ChangeKind)
    private enum class ChangeKind { UPDATE, ADD, REMOVE }

    private fun buildChangeEntries(
        updateFiles: List<String>,
        addFiles: List<String>,
        removeFiles: List<String>,
    ): List<ChangeEntry> = buildList {
        updateFiles.forEach { add(ChangeEntry(it, ChangeKind.UPDATE)) }
        addFiles.forEach { add(ChangeEntry(it, ChangeKind.ADD)) }
        removeFiles.forEach { add(ChangeEntry(it, ChangeKind.REMOVE)) }
    }

    private fun groupByTopDir(entries: List<ChangeEntry>): Map<String, List<ChangeEntry>> =
        entries.groupBy { it.rel.substringBefore('/') }

    private fun formatDirSummary(entries: List<ChangeEntry>): String {
        val updates = entries.count { it.kind == ChangeKind.UPDATE }
        val adds = entries.count { it.kind == ChangeKind.ADD }
        val removes = entries.count { it.kind == ChangeKind.REMOVE }
        val parts = buildList {
            if (updates > 0) add("change $updates")
            if (adds > 0) add("+$adds")
            if (removes > 0) add("-$removes")
        }
        return parts.joinToString(", ")
    }

    private fun diffPathFor(snapshotDir: File, expectedRel: String): String? {
        val diffRel = expectedRel.replace(".expected.", ".diff.")
        if (diffRel != expectedRel && File(snapshotDir, diffRel).exists()) return diffRel
        return null
    }

    private fun StringBuilder.appendUpdateEntry(snapshotDir: File, rel: String) {
        val actualRel = actualPathFor(snapshotDir, rel)
        val diffRel = diffPathFor(snapshotDir, rel)
        appendLine("<details>")
        appendLine("<summary> 🟦 <code>$rel</code> </summary>")
        appendLine()
        appendLine("| before | after |")
        appendLine("|---|---|")
        append("| <code>build/snapshots/$rel</code> | ")
        if (actualRel != null) {
            appendLine("<code>build/snapshots/$actualRel</code> |")
        } else {
            appendLine("(NONE) |")
        }
        appendLine()
        if (isTextFile(rel) && actualRel != null) {
            appendBeforeAfterTable(
                before = File(snapshotDir, rel).readText(),
                after = File(snapshotDir, actualRel).readText(),
            )
            if (diffRel != null) {
                appendDiffTable(File(snapshotDir, diffRel).readText())
            }
        }
        appendLine()
        appendLine("</details>")
        appendLine()
    }

    private fun StringBuilder.appendAddEntry(snapshotDir: File, rel: String) {
        appendLine("<details>")
        appendLine("<summary> 🟢 <code>$rel</code> </summary>")
        appendLine()
        appendLine("| before | after |")
        appendLine("|---|---|")
        appendLine("| (NONE) | <code>build/snapshots/$rel</code> |")
        appendLine()
        if (isTextFile(rel)) {
            val file = File(snapshotDir, rel)
            if (file.exists()) {
                val content = file.readText()
                appendBeforeAfterTable(before = null, after = content)
            }
        }
        appendLine("</details>")
        appendLine()
    }

    private fun removedPathFor(snapshotDir: File, expectedRel: String): String? {
        val removedRel = expectedRel.replace(".expected.", ".removed.")
        if (removedRel != expectedRel && File(snapshotDir, removedRel).exists()) return removedRel
        return null
    }

    private fun StringBuilder.appendRemoveEntry(snapshotDir: File, rel: String) {
        appendLine("<details>")
        appendLine("<summary> ❌ <code>$rel</code> </summary>")
        appendLine()
        appendLine("| before | after |")
        appendLine("|---|---|")
        appendLine("| <code>build/snapshots/$rel</code> | (NONE) |")
        appendLine()
        if (isTextFile(rel)) {
            val removedRel = removedPathFor(snapshotDir, rel)
            if (removedRel != null) {
                val content = File(snapshotDir, removedRel).readText()
                appendBeforeAfterTable(before = content, after = null)
            }
        }
        appendLine("</details>")
        appendLine()
    }

    private fun StringBuilder.appendBeforeAfterTable(before: String?, after: String?) {
        appendLine("<table>")
        appendLine()
        appendLine("<tr>")
        appendLine("<th>before</th> <th>after</th>")
        appendLine("</tr>")
        appendLine()
        appendLine("<tr>")
        appendLine()
        appendLine("<td>")
        appendLine()
        if (before != null) {
            appendLine("```")
            append(before)
            if (!before.endsWith("\n")) appendLine()
            appendLine("```")
        }
        appendLine()
        appendLine("</td>")
        appendLine()
        appendLine("<td>")
        appendLine()
        if (after != null) {
            appendLine("```")
            append(after)
            if (!after.endsWith("\n")) appendLine()
            appendLine("```")
        }
        appendLine()
        appendLine("</td>")
        appendLine()
        appendLine("</tr>")
        appendLine()
        appendLine("</table>")
        appendLine()
    }

    // --- HTML generation ---

    private fun buildResultHtml(
        snapshotDir: File,
        allFiles: List<String>,
        addFiles: List<String>,
        removeFiles: List<String>,
        updateFiles: List<String>,
    ): String = buildString {
        val totalChanges = updateFiles.size + addFiles.size + removeFiles.size
        val reproduceCmd = buildString {
            append("./tools/snapshot-diff.sh")
            append(" \\\n  -before=${beforeRef.get()}")
            val after = afterRef.get()
            if (after != "current working tree") append(" \\\n  -after=$after")
            pbtIteration.orNull?.let { append(" \\\n  -pbt-iteration=$it") }
        }

        appendLine("<!DOCTYPE html>")
        appendLine("<html lang=\"ja\">")
        appendLine("<head>")
        appendLine("<meta charset=\"UTF-8\">")
        appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        appendLine("<title>Snapshot diff report</title>")
        appendLine("<style>")
        appendLine(CSS)
        appendLine("</style>")
        appendLine("</head>")
        appendLine("<body>")

        appendLine("<h2>Snapshot diff report</h2>")

        // Summary table
        appendLine("<div class=\"summary-grid\">")
        appendLine("<div class=\"summary-card\">")
        appendLine("<h3>Summary</h3>")
        appendLine("<table class=\"summary-table\">")
        appendLine("<thead><tr><th></th><th>Count</th></tr></thead>")
        appendLine("<tbody>")
        appendLine("<tr><td>Total snapshots</td><td>${allFiles.size}</td></tr>")
        appendLine("<tr><td>\uD83D\uDFE6 Update</td><td>${updateFiles.size}</td></tr>")
        appendLine("<tr><td>\uD83D\uDFE2 Add</td><td>${addFiles.size}</td></tr>")
        appendLine("<tr><td>❌ Remove</td><td>${removeFiles.size}</td></tr>")
        appendLine("</tbody></table>")
        appendLine("</div>")

        appendLine("<div class=\"summary-card\">")
        appendLine("<h3>再現方法</h3>")
        appendLine("<pre><code>${htmlEscape(reproduceCmd)}</code></pre>")
        appendLine("</div>")
        appendLine("</div>")

        // Snapshot entries
        if (totalChanges == 0) {
            appendLine("<h3>Snapshots</h3>")
            appendLine("<p>✅ No snapshot change!</p>")
        } else {
            appendLine("<h3>Snapshots ($totalChanges)</h3>")
            val entries = buildChangeEntries(updateFiles, addFiles, removeFiles)
            val grouped = groupByTopDir(entries)
            for ((dir, dirEntries) in grouped) {
                appendLine("<details class=\"dir-group\">")
                appendLine("<summary><b>$dir</b> <span class=\"dir-count\">(${formatDirSummary(dirEntries)})</span></summary>")
                appendLine("<table class=\"snapshot-table\">")
                for (entry in dirEntries) {
                    when (entry.kind) {
                        ChangeKind.UPDATE -> appendHtmlUpdateRow(snapshotDir, entry.rel)
                        ChangeKind.ADD -> appendHtmlAddRow(snapshotDir, entry.rel)
                        ChangeKind.REMOVE -> appendHtmlRemoveRow(snapshotDir, entry.rel)
                    }
                }
                appendLine("</table>")
                appendLine("</details>")
            }
        }

        appendLine("</body>")
        appendLine("</html>")
    }

    private fun StringBuilder.appendHtmlCellContent(snapshotDir: File, rel: String?) {
        if (rel == null) {
            appendLine("<td></td>")
        } else if (isImageFile(rel)) {
            appendLine("<td><div class=\"img-skeleton\"><img src=\"$rel\" onload=\"this.parentElement.classList.add('loaded')\"></div></td>")
        } else if (isTextFile(rel)) {
            val file = File(snapshotDir, rel)
            val content = if (file.exists()) htmlEscape(file.readText()) else ""
            appendLine("<td><pre>$content</pre></td>")
        } else {
            appendLine("<td><pre>(binary)</pre></td>")
        }
    }

    private fun StringBuilder.appendHtmlUpdateRow(snapshotDir: File, rel: String) {
        val actualRel = actualPathFor(snapshotDir, rel)
        val diffRel = diffPathFor(snapshotDir, rel)
        appendLine("<tr class=\"label-row update\"><td colspan=\"3\">\uD83D\uDFE6 <code>$rel</code></td></tr>")
        appendLine("<tr class=\"header-row\"><th>before</th><th>diff</th><th>after</th></tr>")
        appendLine("<tr>")
        appendHtmlCellContent(snapshotDir, rel)
        appendHtmlDiffCell(snapshotDir, diffRel)
        appendHtmlCellContent(snapshotDir, actualRel)
        appendLine("</tr>")
    }

    private fun StringBuilder.appendHtmlAddRow(snapshotDir: File, rel: String) {
        appendLine("<tr class=\"label-row add\"><td colspan=\"3\">\uD83D\uDFE2 <code>$rel</code></td></tr>")
        appendLine("<tr class=\"header-row\"><th>before</th><th>diff</th><th>after</th></tr>")
        appendLine("<tr>")
        appendLine("<td></td>")
        appendLine("<td></td>")
        appendHtmlCellContent(snapshotDir, rel)
        appendLine("</tr>")
    }

    private fun StringBuilder.appendHtmlRemoveRow(snapshotDir: File, rel: String) {
        val removedRel = removedPathFor(snapshotDir, rel)
        appendLine("<tr class=\"label-row remove\"><td colspan=\"3\">❌ <code>$rel</code></td></tr>")
        appendLine("<tr class=\"header-row\"><th>before</th><th>diff</th><th>after</th></tr>")
        appendLine("<tr>")
        appendHtmlCellContent(snapshotDir, removedRel ?: rel)
        appendLine("<td></td>")
        appendLine("<td></td>")
        appendLine("</tr>")
    }

    private fun StringBuilder.appendHtmlDiffCell(snapshotDir: File, diffRel: String?) {
        if (diffRel == null) {
            appendLine("<td></td>")
        } else if (isImageFile(diffRel)) {
            appendLine("<td><div class=\"img-skeleton\"><img src=\"$diffRel\" onload=\"this.parentElement.classList.add('loaded')\"></div></td>")
        } else if (isTextFile(diffRel)) {
            val content = File(snapshotDir, diffRel).readText()
            appendLine("<td><pre class=\"diff-content\">${colorDiff(htmlEscape(content))}</pre></td>")
        } else {
            appendLine("<td><pre>(binary)</pre></td>")
        }
    }

    private fun htmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun colorDiff(escapedDiff: String): String = escapedDiff.lines().joinToString("\n") { line ->
        when {
            line.startsWith("+") -> "<span class=\"diff-add\">$line</span>"
            line.startsWith("-") -> "<span class=\"diff-del\">$line</span>"
            line.startsWith("@") -> "<span class=\"diff-hunk\">$line</span>"
            else -> line
        }
    }

    private fun StringBuilder.appendDiffTable(diffContent: String) {
        appendLine("<table>")
        appendLine()
        appendLine("<tr> <th> diff </th> </tr>")
        appendLine()
        appendLine("<tr>")
        appendLine("<td>")
        appendLine()
        appendLine("```diff")
        append(diffContent)
        if (!diffContent.endsWith("\n")) appendLine()
        appendLine("```")
        appendLine()
        appendLine("</td>")
        appendLine("</tr>")
        appendLine()
        appendLine("</table>")
        appendLine()
    }
}

private val CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
    line-height: 1.5;
    color: #1f2328;
    max-width: 1200px;
    margin: 0 auto;
    padding: 24px;
    background: #fff;
}
h2 { margin-bottom: 16px; border-bottom: 1px solid #d1d9e0; padding-bottom: 8px; }
h3 { margin: 0 0 8px; }
h4 { margin: 8px 0 4px; font-size: 14px; }
code { background: #eff1f3; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
pre { white-space: pre-wrap; word-break: break-all; font-size: 12px; line-height: 1.4; }
pre code { background: none; padding: 0; }
.summary-grid { display: grid; grid-template-columns: auto 1fr; gap: 16px; margin-bottom: 24px; }
.summary-card { border: 1px solid #d1d9e0; border-radius: 8px; padding: 16px; }
.summary-card pre { background: #f6f8fa; padding: 12px; border-radius: 6px; }
.summary-table { width: 100%; border-collapse: collapse; margin-top: 8px; }
.summary-table th, .summary-table td {
    text-align: left; padding: 4px 12px; border-bottom: 1px solid #d1d9e0;
}
.snapshot-table {
    width: 100%; border-collapse: collapse; table-layout: fixed;
    border: 1px solid #d1d9e0;
}
.snapshot-table th, .snapshot-table td {
    border: 1px solid #d1d9e0; padding: 8px; vertical-align: top;
}
.snapshot-table th { background: #f6f8fa; font-size: 13px; }
.snapshot-table pre {
    background: #f6f8fa; padding: 8px; border-radius: 4px;
    overflow-x: auto; font-size: 12px; margin: 0;
}
.diff-add { color: #1a7f37; background: #dafbe1; }
.diff-del { color: #cf222e; background: #ffebe9; }
.diff-hunk { color: #656d76; }
.label-row td {
    font-size: 14px; font-weight: 600; padding: 10px 12px;
}
.label-row.update td { background: #ddf4ff; }
.label-row.add td { background: #dafbe1; }
.label-row.remove td { background: #ffebe9; }
.header-row th { font-size: 12px; font-weight: 600; padding: 4px 8px; }
.snapshot-table img { max-width: 100%; height: auto; display: block; }
.img-skeleton {
    position: relative; overflow: hidden; background: #eee;
    min-height: 40px;
}
.img-skeleton::before {
    content: ""; position: absolute; inset: 0;
    background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
    background-size: 400% 100%;
    animation: skeleton 1.4s ease infinite;
}
.img-skeleton.loaded::before { display: none; }
.img-skeleton.loaded { background: none; }
@keyframes skeleton {
    0% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
}
.dir-group { border: 1px solid #d1d9e0; border-radius: 8px; margin: 12px 0; overflow: hidden; }
.dir-group > summary {
    padding: 10px 14px; cursor: pointer; font-size: 15px;
    background: #f6f8fa; border-bottom: 1px solid #d1d9e0;
}
.dir-group > .snapshot-table { border-top: none; border-radius: 0; }
.dir-count { color: #656d76; font-weight: normal; }
""".trimIndent()
