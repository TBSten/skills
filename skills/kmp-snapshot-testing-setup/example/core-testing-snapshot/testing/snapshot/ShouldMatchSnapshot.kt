package com.example.snapshot.testing.snapshot

import io.kotest.assertions.withClue
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestScope
import io.kotest.matchers.booleans.shouldBeTrue
import com.example.snapshot.core.printOnlyDebug
import com.example.snapshot.testing.snapshot.internal.SnapshotRegistry
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private data class ShouldMatchSnapshotClue(
    val snapshotFilePath: String,
    val snapshotTestFlavor: String,
) {
    override fun toString(): String = buildString {
        appendLine("snapshotFilePath = $snapshotFilePath")
        appendLine("snapshotTestFlavor = $snapshotTestFlavor")
    }
}

fun TestScope.shouldMatchSnapshot(
    fileName: String,
    extension: String,
    record: RecordContext.() -> Unit,
    verify: VerifyContext.() -> Unit,
) {
    val snapshotPath = buildSnapshotPath()
    val flavor = snapshotTestFlavor()
    val files = snapshotFiles(snapshotPath, fileName, extension)

    withClue(
        ShouldMatchSnapshotClue(
            snapshotFilePath = files.expected.absolutePath,
            snapshotTestFlavor = flavor.name,
        ),
    ) {
        when (flavor) {
            SnapshotTestFlavor.Record -> record(
                file = files.expected,
                record = record,
            )

            SnapshotTestFlavor.Verify -> {
                if (!files.expected.exists()) {
                    // .expected がない = 新規テスト。.actual に記録して ADD 検出可能にする
                    // error() は投げず続行し、PBT が全 case 走りきるようにする
                    record(file = files.actual, record = record)
                    SnapshotRegistry.markUsed(files.expected)
                    printOnlyDebug("[Snapshot] WARNING: 新規スナップショット (expected 未作成): ${files.expected.absolutePath}")
                    return@withClue
                }
                verify(
                    files = files,
                    verify = verify,
                )
            }
        }
    }
}

internal fun TestScope.buildSnapshotPath(): String {
    val parts = mutableListOf<String>()
    var current: TestCase? = testCase
    while (current != null) {
        val name = current.name.run { "${prefix ?: ""}${this.name}${suffix ?: ""}" }
        parts.add(name)
        current = current.parent
    }
    parts.add(testCase.spec::class.simpleName ?: "UnknownSpec")
    parts.reverse()
    return parts.joinToString(File.separator)
}

private fun record(
    file: File,
    record: RecordContext.() -> Unit,
) {
    val outputStream = file
        .also { it.parentFile.mkdirs() }
        .also {
            if (it.exists()) it.delete()
            it.createNewFile()
        }.outputStream()

    record.invoke(RecordContext(outputStream))

    printOnlyDebug("[Snapshot] Recorded: ${file.absolutePath}")
}

private fun verify(
    files: SnapshotFiles,
    verify: VerifyContext.() -> Unit,
) {
    SnapshotRegistry.markUsed(files.expected)
    withClue(
        "スナップショットが未作成です: ${files.expected.absolutePath}\n" +
                "./gradlew jvmSnapshotTestRecord を実行してください",
    ) {
        files.expected.exists().shouldBeTrue()
    }
    verify.invoke(
        VerifyContext(
            inputStream = files.expected.inputStream(),
            expectedFile = files.expected,
            actualFile = files.actual,
            diffFile = files.diff,
        ),
    )
}

internal data class SnapshotFiles(
    val expected: File,
    val actual: File,
    val diff: File,
)

internal fun snapshotDir(snapshotPath: String): File {
    val snapshotTestOutputDir = File(System.getProperty("snapshot-test-output-dir"))
    return snapshotTestOutputDir.resolve(snapshotPath)
}

internal fun snapshotFiles(
    snapshotPath: String,
    fileName: String,
    extension: String,
): SnapshotFiles {
    val dir = snapshotDir(snapshotPath)
    return SnapshotFiles(
        expected = dir.resolve("$fileName.expected$extension"),
        actual = dir.resolve("$fileName.actual$extension"),
        diff = dir.resolve("$fileName.diff$extension"),
    )
}

internal enum class SnapshotTestFlavor {
    Record,
    Verify,
}

internal fun snapshotTestFlavor(): SnapshotTestFlavor {
    val value = System.getProperty("snapshot-test-flavor", "<not-configured>")
    return when (value) {
        "record" -> SnapshotTestFlavor.Record
        "verify" -> SnapshotTestFlavor.Verify
        else -> error("[SnapshotTesting/Setup] Invalid `snapshot-test-flavor` system property: $value")
    }
}

class RecordContext(
    // TODO migrate to kotlinx-io or okio
    val outputStream: OutputStream,
)

class VerifyContext(
    // TODO migrate to kotlinx-io or okio
    val inputStream: InputStream,
    val expectedFile: File,
    val actualFile: File,
    val diffFile: File,
)
