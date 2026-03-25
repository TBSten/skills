package com.example.snapshot.testing.snapshot

import io.kotest.core.test.TestScope
import com.example.snapshot.testing.snapshot.internal.SnapshotRegistry
import java.io.File

private val CASE_DIR_PATTERN = Regex("^case_\\d+.*$")
private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")
private const val MAX_TEXT_PREVIEW_LENGTH = 300

internal val PBT_REPORT_FILE_NAMES = setOf(
    "record-report.md", "record-report.html",
    "verify-report.md", "verify-report.html",
)

internal fun TestScope.generatePbtSnapshotReport(iterationCount: Int) {
    val snapshotPath = buildSnapshotPath()
    val dir = snapshotDir(snapshotPath)
    if (!dir.exists()) return

    val flavor = snapshotTestFlavor()
    val cases = findCaseDirs(dir)
    val testName = testCase.spec::class.simpleName ?: testCase.name.name
    val seed = readSeed(dir)
    val reproduceCommand = buildReproduceCommand(iterationCount)

    when (flavor) {
        SnapshotTestFlavor.Record -> {
            writeAndRegister(dir, "record-report.md", buildRecordReportMd(testName, seed, cases, iterationCount, reproduceCommand))
            writeAndRegister(dir, "record-report.html", buildRecordReportHtml(testName, seed, dir, cases, iterationCount, reproduceCommand))
        }

        SnapshotTestFlavor.Verify -> {
            writeAndRegister(dir, "verify-report.md", buildVerifyReportMd(testName, seed, cases, iterationCount, reproduceCommand))
            writeAndRegister(dir, "verify-report.html", buildVerifyReportHtml(testName, seed, dir, cases, iterationCount, reproduceCommand))
            registerIfExists(dir, "record-report.md")
            registerIfExists(dir, "record-report.html")
        }
    }
}

private fun TestScope.buildReproduceCommand(iterationCount: Int): String {
    val fqcn = testCase.spec::class.qualifiedName ?: return ""
    val testFilter = "--tests='$fqcn'"
    val iterationOpt = System.getProperty("pbt.iteration.count")?.let { " -pbt-iteration=$it" } ?: ""
    return "./tools/snapshot-diff.sh --record-args=\"$testFilter\" --verify-args=\"$testFilter\"$iterationOpt"
}

private fun readSeed(dir: File): Long? {
    val file = dir.resolve("_snapshot_pbt_seed.txt")
    if (!file.exists()) return null
    return file.readText().trim().toLongOrNull()
}

// region Internals

private fun writeAndRegister(dir: File, name: String, content: String) {
    val file = dir.resolve(name)
    file.writeText(content)
    SnapshotRegistry.markUsed(file)
}

private fun registerIfExists(dir: File, name: String) {
    val file = dir.resolve(name)
    if (file.exists()) SnapshotRegistry.markUsed(file)
}

private data class CaseInfo(
    val name: String,
    val dir: File,
    val files: List<String>,
) {
    val label: String get() = name.removePrefix("case_").let { raw ->
        val idx = raw.takeWhile { it.isDigit() }
        val meta = raw.removePrefix(idx).removePrefix("_")
        if (meta.isEmpty()) "#$idx" else "#$idx $meta"
    }

    val imageFiles: List<String> get() = files.filter { isImageFile(it) }
    val textFiles: List<String> get() = files.filter { !isImageFile(it) && it.contains(".expected") }
    val hasImages: Boolean get() = imageFiles.isNotEmpty()
}

private fun findCaseDirs(pbtDir: File): List<CaseInfo> =
    pbtDir.listFiles()
        ?.filter { it.isDirectory && CASE_DIR_PATTERN.matches(it.name) }
        ?.sortedBy { it.name }
        ?.map { caseDir ->
            CaseInfo(
                name = caseDir.name,
                dir = caseDir,
                files = caseDir.walkTopDown()
                    .filter { it.isFile }
                    .map { it.relativeTo(caseDir).path }
                    .sorted()
                    .toList(),
            )
        }
        ?: emptyList()

private fun isImageFile(path: String): Boolean =
    path.substringAfterLast('.', "") in IMAGE_EXTENSIONS

private fun readTextFull(caseDir: File, relPath: String): String {
    val file = caseDir.resolve(relPath)
    if (!file.exists()) return ""
    return file.readText()
}

private fun readTextPreview(caseDir: File, relPath: String): String {
    val text = readTextFull(caseDir, relPath)
    return if (text.length > MAX_TEXT_PREVIEW_LENGTH) {
        text.take(MAX_TEXT_PREVIEW_LENGTH) + "..."
    } else {
        text
    }
}

// endregion

// region Record Report (MD)

private fun buildRecordReportMd(
    testName: String,
    seed: Long?,
    cases: List<CaseInfo>,
    iterationCount: Int,
    reproduceCommand: String,
): String = buildString {
    appendLine("# $testName: PBT Record Report")
    appendLine()
    appendLine("Recorded **${cases.size}** / $iterationCount cases.")
    if (seed != null) appendLine("Seed: `$seed`")
    if (reproduceCommand.isNotEmpty()) appendLine("Reproduce: `$reproduceCommand`")
    appendLine()
    if (cases.isEmpty()) return@buildString
    appendLine("| # | Case | Files |")
    appendLine("|--:|------|-------|")
    cases.forEachIndexed { i, case ->
        val fileNames = case.files
            .filter { it.contains(".expected.") || it.contains(".expected-") }
            .joinToString(", ") { "`${it.substringAfterLast('/')}`" }
        appendLine("| ${i + 1} | `${case.name}` | $fileNames |")
    }
}

// endregion

// region Record Report (HTML)

private fun buildRecordReportHtml(
    testName: String,
    seed: Long?,
    pbtDir: File,
    cases: List<CaseInfo>,
    iterationCount: Int,
    reproduceCommand: String,
): String = buildString {
    val title = "$testName: PBT Record Report"
    appendHtmlHead(title)
    appendLine("<h2>${htmlEscape(title)}</h2>")
    appendLine("<p>Recorded <strong>${cases.size}</strong> / $iterationCount cases.</p>")
    if (seed != null) appendLine("<p class=\"seed\">Seed: <code>$seed</code></p>")
    if (reproduceCommand.isNotEmpty()) {
        appendLine("<div class=\"reproduce-cmd\"><code>${htmlEscape(reproduceCommand)}</code></div>")
    }

    if (cases.isEmpty()) {
        appendHtmlFoot()
        return@buildString
    }

    appendGallery(pbtDir, cases)
    appendHtmlFoot()
}

private fun StringBuilder.appendGallery(pbtDir: File, cases: List<CaseInfo>) {
    val hasAnyImages = cases.any { it.hasImages }
    if (hasAnyImages) {
        appendImageGallery(cases)
    } else {
        appendTextGallery(cases)
    }
}

private fun StringBuilder.appendImageGallery(cases: List<CaseInfo>) {
    appendLine("<div class=\"gallery\">")
    for ((idx, case) in cases.withIndex()) {
        appendLine("<div class=\"gallery-card\" data-modal=\"modal-$idx\" role=\"button\" tabindex=\"0\">")
        val screenshot = case.imageFiles.firstOrNull { it.contains("screenshot") }
            ?: case.imageFiles.firstOrNull()
        if (screenshot != null) {
            appendLine("<div class=\"gallery-img\">")
            appendLine("<span class=\"checkerboard\"><img src=\"${case.name}/$screenshot\" loading=\"lazy\" alt=\"${case.label}\"></span>")
            appendLine("</div>")
        }
        appendLine("<div class=\"gallery-label\">${htmlEscape(case.label)}</div>")
        if (case.imageFiles.size > 1 || case.textFiles.isNotEmpty()) {
            appendLine("<div class=\"gallery-meta\">")
            appendLine("${case.imageFiles.size} images, ${case.textFiles.size} text")
            appendLine("</div>")
        }
        appendLine("</div>")
    }
    appendLine("</div>")
    // Modals
    for ((idx, case) in cases.withIndex()) {
        appendCaseModal("modal-$idx", case)
    }
    appendModalScript()
}

private fun StringBuilder.appendTextGallery(cases: List<CaseInfo>) {
    appendLine("<div class=\"text-gallery\">")
    for (case in cases) {
        appendLine("<details class=\"text-card\">")
        appendLine("<summary><code>${htmlEscape(case.label)}</code>")
        appendLine("<span class=\"file-count\">${case.textFiles.size} files</span></summary>")
        appendLine("<div class=\"text-card-body\">")
        for (textFile in case.textFiles) {
            val preview = readTextPreview(case.dir, textFile)
            val fileName = textFile.substringAfterLast('/')
            appendLine("<div class=\"text-file\">")
            appendLine("<div class=\"text-file-name\">${htmlEscape(fileName)}</div>")
            appendLine("<pre>${htmlEscape(preview)}</pre>")
            appendLine("</div>")
        }
        appendLine("</div>")
        appendLine("</details>")
    }
    appendLine("</div>")
}

// endregion

// region Verify Report (MD)

private enum class CaseStatus { PASS, CHANGED, NEW }

private data class CaseVerifyResult(
    val case: CaseInfo,
    val status: CaseStatus,
    val changedFiles: List<String>,
)

private fun analyzeVerifyResults(cases: List<CaseInfo>): List<CaseVerifyResult> =
    cases.map { case ->
        val actualFiles = case.files.filter { it.contains(".actual.") }
        val hasExpected = case.files.any { it.contains(".expected.") }
        val status = when {
            actualFiles.isEmpty() -> CaseStatus.PASS
            hasExpected -> CaseStatus.CHANGED
            else -> CaseStatus.NEW
        }
        CaseVerifyResult(case, status, actualFiles)
    }

private fun buildVerifyReportMd(
    testName: String,
    seed: Long?,
    cases: List<CaseInfo>,
    iterationCount: Int,
    reproduceCommand: String,
): String = buildString {
    val results = analyzeVerifyResults(cases)
    val passed = results.count { it.status == CaseStatus.PASS }
    val changed = results.count { it.status == CaseStatus.CHANGED }
    val new = results.count { it.status == CaseStatus.NEW }

    appendLine("# $testName: PBT Verify Report")
    appendLine()
    appendLine("**${results.size}** / $iterationCount cases: $passed passed, $changed changed, $new new")
    if (seed != null) appendLine("Seed: `$seed`")
    if (reproduceCommand.isNotEmpty()) appendLine("Reproduce: `$reproduceCommand`")
    appendLine()

    val nonPassResults = results.filter { it.status != CaseStatus.PASS }
    if (nonPassResults.isNotEmpty()) {
        appendLine("## Changed Cases")
        appendLine()
        for (result in nonPassResults) {
            val label = if (result.status == CaseStatus.CHANGED) "CHANGED" else "NEW"
            appendLine("### ${result.case.name} ($label)")
            appendLine()
            for (actual in result.changedFiles) {
                appendLine("- `$actual`")
            }
            appendLine()
        }
    }

    appendLine("## All Cases")
    appendLine()
    appendLine("| # | Case | Status |")
    appendLine("|--:|------|--------|")
    results.forEachIndexed { i, result ->
        val status = when (result.status) {
            CaseStatus.PASS -> "Pass"
            CaseStatus.CHANGED -> "**Changed**"
            CaseStatus.NEW -> "**New**"
        }
        appendLine("| ${i + 1} | `${result.case.name}` | $status |")
    }
}

// endregion

// region Verify Report (HTML)

private fun buildVerifyReportHtml(
    testName: String,
    seed: Long?,
    pbtDir: File,
    cases: List<CaseInfo>,
    iterationCount: Int,
    reproduceCommand: String,
): String = buildString {
    val results = analyzeVerifyResults(cases)
    val passed = results.count { it.status == CaseStatus.PASS }
    val changed = results.count { it.status == CaseStatus.CHANGED }
    val new = results.count { it.status == CaseStatus.NEW }

    val title = "$testName: PBT Verify Report"
    appendHtmlHead(title)
    appendLine("<h2>${htmlEscape(title)}</h2>")
    appendLine(
        "<p><strong>${results.size}</strong> / $iterationCount cases: " +
            "<span class=\"badge pass\">$passed passed</span> " +
            "<span class=\"badge changed\">$changed changed</span> " +
            "<span class=\"badge new\">$new new</span></p>",
    )
    if (seed != null) appendLine("<p class=\"seed\">Seed: <code>$seed</code></p>")
    if (reproduceCommand.isNotEmpty()) {
        appendLine("<div class=\"reproduce-cmd\"><code>${htmlEscape(reproduceCommand)}</code></div>")
    }

    if (results.isEmpty()) {
        appendHtmlFoot()
        return@buildString
    }

    // Filter buttons
    appendLine("<div class=\"filter-bar\">")
    appendLine("<button class=\"filter-btn active\" data-filter=\"all\">All</button>")
    if (changed > 0) appendLine("<button class=\"filter-btn\" data-filter=\"changed\">Changed ($changed)</button>")
    if (new > 0) appendLine("<button class=\"filter-btn\" data-filter=\"new\">New ($new)</button>")
    if (passed > 0) appendLine("<button class=\"filter-btn\" data-filter=\"pass\">Passed ($passed)</button>")
    appendLine("</div>")

    val hasAnyImages = results.any { it.case.hasImages }
    if (hasAnyImages) {
        appendVerifyImageGallery(results)
    } else {
        appendVerifyTextGallery(results)
    }

    appendFilterScript()
    appendHtmlFoot()
}

private fun StringBuilder.appendVerifyImageGallery(results: List<CaseVerifyResult>) {
    appendLine("<div class=\"gallery\">")
    for ((idx, result) in results.withIndex()) {
        val case = result.case
        val statusCss = result.status.name.lowercase()
        appendLine("<div class=\"gallery-card $statusCss\" data-status=\"$statusCss\" data-modal=\"vmodal-$idx\" role=\"button\" tabindex=\"0\">")

        val screenshot = case.imageFiles.firstOrNull { it.contains("screenshot") }
            ?: case.imageFiles.firstOrNull()
        if (screenshot != null) {
            appendLine("<div class=\"gallery-img\">")
            appendLine("<span class=\"checkerboard\"><img src=\"${case.name}/$screenshot\" loading=\"lazy\" alt=\"${case.label}\"></span>")
            appendLine("</div>")
        }
        appendLine("<div class=\"gallery-label\">${htmlEscape(case.label)}</div>")
        appendLine("<div class=\"gallery-status $statusCss\">${result.status.name}</div>")
        appendLine("</div>")
    }
    appendLine("</div>")
    // Modals
    for ((idx, result) in results.withIndex()) {
        appendCaseModal("vmodal-$idx", result.case, result.status)
    }
    appendModalScript()
}

private fun StringBuilder.appendVerifyTextGallery(results: List<CaseVerifyResult>) {
    appendLine("<div class=\"text-gallery\">")
    for (result in results) {
        val case = result.case
        val statusCss = result.status.name.lowercase()
        appendLine("<details class=\"text-card $statusCss\" data-status=\"$statusCss\">")
        appendLine("<summary><code>${htmlEscape(case.label)}</code>")
        appendLine("<span class=\"badge $statusCss\">${result.status.name}</span></summary>")
        appendLine("<div class=\"text-card-body\">")
        for (textFile in case.textFiles) {
            val preview = readTextPreview(case.dir, textFile)
            val fileName = textFile.substringAfterLast('/')
            appendLine("<div class=\"text-file\">")
            appendLine("<div class=\"text-file-name\">${htmlEscape(fileName)}</div>")
            appendLine("<pre>${htmlEscape(preview)}</pre>")
            appendLine("</div>")
        }
        if (result.changedFiles.isNotEmpty()) {
            appendLine("<div class=\"changed-files\">")
            appendLine("<strong>Changed:</strong>")
            appendLine("<ul>")
            for (f in result.changedFiles) {
                appendLine("<li><code>${htmlEscape(f)}</code></li>")
            }
            appendLine("</ul>")
            appendLine("</div>")
        }
        appendLine("</div>")
        appendLine("</details>")
    }
    appendLine("</div>")
}

private fun StringBuilder.appendCaseModal(
    id: String,
    case: CaseInfo,
    status: CaseStatus? = null,
) {
    appendLine("<div class=\"modal-overlay\" id=\"$id\">")
    appendLine("<div class=\"modal\">")
    appendLine("<div class=\"modal-header\">")
    appendLine("<h3>${htmlEscape(case.label)}</h3>")
    if (status != null) {
        val css = status.name.lowercase()
        appendLine("<span class=\"badge $css\">${status.name}</span>")
    }
    appendLine("<button class=\"modal-close\">&times;</button>")
    appendLine("</div>")
    appendLine("<div class=\"modal-body\">")

    // All images
    for (img in case.imageFiles) {
        val fileName = img.substringAfterLast('/')
        appendLine("<div class=\"modal-section\">")
        appendLine("<div class=\"modal-file-name\">${htmlEscape(fileName)}</div>")
        appendLine("<div class=\"modal-img\">")
        appendLine("<span class=\"checkerboard\"><img src=\"${case.name}/$img\" loading=\"lazy\" alt=\"${htmlEscape(fileName)}\"></span>")
        appendLine("</div>")
        appendLine("</div>")
    }

    // All text files
    for (textFile in case.textFiles) {
        val fileName = textFile.substringAfterLast('/')
        val content = readTextFull(case.dir, textFile)
        appendLine("<div class=\"modal-section\">")
        appendLine("<div class=\"modal-file-name\">${htmlEscape(fileName)}</div>")
        appendLine("<pre>${htmlEscape(content)}</pre>")
        appendLine("</div>")
    }

    // Diff / actual files for verify
    val diffFiles = case.files.filter { it.contains(".diff.") || it.contains(".actual.") }
    if (diffFiles.isNotEmpty()) {
        appendLine("<h4>Changes</h4>")
        for (f in diffFiles) {
            val fileName = f.substringAfterLast('/')
            appendLine("<div class=\"modal-section\">")
            appendLine("<div class=\"modal-file-name\">${htmlEscape(fileName)}</div>")
            if (isImageFile(f)) {
                appendLine("<div class=\"modal-img\">")
                appendLine("<span class=\"checkerboard\"><img src=\"${case.name}/$f\" loading=\"lazy\" alt=\"${htmlEscape(fileName)}\"></span>")
                appendLine("</div>")
            } else {
                val content = readTextFull(case.dir, f)
                appendLine("<pre>${htmlEscape(content)}</pre>")
            }
            appendLine("</div>")
        }
    }

    appendLine("</div>") // modal-body
    appendLine("</div>") // modal
    appendLine("</div>") // modal-overlay
}

private fun StringBuilder.appendModalScript() {
    appendLine("<script>")
    appendLine(
        """
        document.querySelectorAll('[data-modal]').forEach(card => {
          card.addEventListener('click', () => {
            document.getElementById(card.dataset.modal).classList.add('active');
          });
          card.addEventListener('keydown', e => {
            if (e.key === 'Enter') document.getElementById(card.dataset.modal).classList.add('active');
          });
        });
        document.querySelectorAll('.modal-overlay').forEach(overlay => {
          overlay.addEventListener('click', e => {
            if (e.target === overlay) overlay.classList.remove('active');
          });
          overlay.querySelector('.modal-close').addEventListener('click', () => {
            overlay.classList.remove('active');
          });
        });
        document.addEventListener('keydown', e => {
          if (e.key === 'Escape') {
            document.querySelectorAll('.modal-overlay.active').forEach(o => o.classList.remove('active'));
          }
        });
        """.trimIndent(),
    )
    appendLine("</script>")
}

private fun StringBuilder.appendFilterScript() {
    appendLine("<script>")
    appendLine(
        """
        document.querySelectorAll('.filter-btn').forEach(btn => {
          btn.addEventListener('click', () => {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const f = btn.dataset.filter;
            document.querySelectorAll('[data-status]').forEach(el => {
              el.style.display = (f === 'all' || el.dataset.status === f) ? '' : 'none';
            });
          });
        });
        """.trimIndent(),
    )
    appendLine("</script>")
}

// endregion

// region HTML Helpers

private fun StringBuilder.appendHtmlHead(title: String) {
    appendLine("<!DOCTYPE html>")
    appendLine("<html lang=\"ja\">")
    appendLine("<head>")
    appendLine("<meta charset=\"UTF-8\">")
    appendLine("<title>$title</title>")
    appendLine("<style>")
    appendLine(PBT_REPORT_CSS)
    appendLine("</style>")
    appendLine("</head>")
    appendLine("<body>")
}

private fun StringBuilder.appendHtmlFoot() {
    appendLine("</body>")
    appendLine("</html>")
}

private fun htmlEscape(s: String): String = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private val PBT_REPORT_CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
    line-height: 1.5; color: #1f2328;
    max-width: 1400px; margin: 0 auto; padding: 24px; background: #fff;
}
h2 { margin-bottom: 12px; border-bottom: 1px solid #d1d9e0; padding-bottom: 8px; }
h3 { margin: 16px 0 8px; }
.seed { font-size: 13px; color: #57606a; margin-top: 4px; }
.reproduce-cmd {
    margin: 8px 0 16px; padding: 8px 12px;
    background: #f6f8fa; border: 1px solid #d1d9e0; border-radius: 6px;
    font-size: 12px; overflow-x: auto;
}
.reproduce-cmd code { background: none; padding: 0; }
code { background: #eff1f3; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
pre {
    white-space: pre-wrap; word-break: break-all;
    font-size: 11px; line-height: 1.3; margin: 0;
    background: #f6f8fa; padding: 8px; border-radius: 4px;
    max-height: 200px; overflow-y: auto;
}

/* Badges */
.badge {
    display: inline-block; padding: 2px 10px; border-radius: 12px;
    font-size: 12px; font-weight: 600;
}
.badge.pass { background: #dafbe1; color: #1a7f37; }
.badge.changed { background: #ffebe9; color: #cf222e; }
.badge.new { background: #ddf4ff; color: #0969da; }

/* Filter bar */
.filter-bar { display: flex; gap: 8px; margin: 16px 0; }
.filter-btn {
    padding: 6px 16px; border: 1px solid #d1d9e0; border-radius: 20px;
    background: #fff; cursor: pointer; font-size: 13px; font-weight: 500;
    transition: all 0.15s;
}
.filter-btn:hover { background: #f6f8fa; }
.filter-btn.active { background: #0969da; color: #fff; border-color: #0969da; }

/* Image gallery */
.gallery {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px; margin: 16px 0;
}
.gallery-card {
    border: 2px solid #d1d9e0; border-radius: 8px; overflow: hidden;
    background: #fff; transition: transform 0.15s, box-shadow 0.15s;
    cursor: pointer;
}
.gallery-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.gallery-card:focus-visible { outline: 2px solid #0969da; outline-offset: 2px; }
.gallery-card.changed { border-color: #cf222e; }
.gallery-card.new { border-color: #0969da; }
.gallery-card.pass { border-color: #d1d9e0; }
.checkerboard {
    display: inline-flex;
    background-image:
        linear-gradient(45deg, #e0e0e0 25%, transparent 25%),
        linear-gradient(-45deg, #e0e0e0 25%, transparent 25%),
        linear-gradient(45deg, transparent 75%, #e0e0e0 75%),
        linear-gradient(-45deg, transparent 75%, #e0e0e0 75%);
    background-size: 16px 16px;
    background-position: 0 0, 0 8px, 8px -8px, -8px 0;
    background-color: #f5f5f5;
    line-height: 0;
}
.checkerboard img { display: block; }
.gallery-img {
    position: relative; overflow: hidden;
    aspect-ratio: 3 / 4;
    display: flex; align-items: center; justify-content: center;
    background: #f8f8f8;
}
.gallery-img .checkerboard { max-width: 100%; max-height: 100%; }
.gallery-img img {
    max-width: 100%; max-height: 100%; object-fit: contain;
}
.gallery-label {
    padding: 6px 10px; font-size: 11px; color: #57606a;
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    border-top: 1px solid #eee;
}
.gallery-status {
    padding: 2px 10px 6px; font-size: 11px; font-weight: 600;
}
.gallery-status.pass { color: #1a7f37; }
.gallery-status.changed { color: #cf222e; }
.gallery-status.new { color: #0969da; }
.gallery-meta {
    padding: 2px 10px 6px; font-size: 11px; color: #8b949e;
}
.gallery-diff {
    padding: 4px; border-top: 1px solid #eee; background: #fff8f8;
}
.gallery-diff img {
    width: 100%; height: auto; display: block;
}

/* Text gallery */
.text-gallery {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 8px; margin: 16px 0;
}
.text-card {
    border: 2px solid #d1d9e0; border-radius: 8px; overflow: hidden;
}
.text-card.changed { border-color: #cf222e; }
.text-card.new { border-color: #0969da; }
.text-card > summary {
    padding: 8px 12px; cursor: pointer; font-size: 13px;
    background: #f6f8fa; display: flex; align-items: center; gap: 8px;
}
.text-card-body { padding: 8px; }
.text-file { margin-bottom: 6px; }
.text-file-name {
    font-size: 11px; font-weight: 600; color: #57606a;
    margin-bottom: 2px;
}
.file-count { font-size: 11px; color: #8b949e; margin-left: auto; }
.changed-files {
    margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee;
    font-size: 12px;
}
.changed-files ul { padding-left: 20px; margin-top: 4px; }

/* Modal */
.modal-overlay {
    display: none; position: fixed; inset: 0;
    background: rgba(0,0,0,0.5); z-index: 1000;
    justify-content: center; align-items: flex-start;
    padding: 40px 20px; overflow-y: auto;
}
.modal-overlay.active { display: flex; }
.modal {
    background: #fff; border-radius: 12px;
    max-width: 900px; width: 100%;
    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
    animation: modalIn 0.2s ease;
}
@keyframes modalIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: none; } }
.modal-header {
    display: flex; align-items: center; gap: 10px;
    padding: 16px 20px; border-bottom: 1px solid #d1d9e0;
}
.modal-header h3 { flex: 1; font-size: 16px; word-break: break-all; }
.modal-close {
    background: none; border: none; font-size: 24px;
    cursor: pointer; color: #57606a; padding: 0 4px; line-height: 1;
}
.modal-close:hover { color: #1f2328; }
.modal-body { padding: 20px; }
.modal-section { margin-bottom: 16px; }
.modal-file-name {
    font-size: 12px; font-weight: 600; color: #57606a;
    margin-bottom: 6px;
}
.modal-img { border-radius: 6px; overflow: hidden; border: 1px solid #d1d9e0; }
.modal-img .checkerboard { display: inline-flex; }
.modal-img img { max-width: 100%; height: auto; object-fit: contain; }
.modal-body h4 {
    font-size: 14px; margin: 16px 0 8px;
    padding-top: 12px; border-top: 1px solid #d1d9e0;
    color: #cf222e;
}
""".trimIndent()

// endregion
