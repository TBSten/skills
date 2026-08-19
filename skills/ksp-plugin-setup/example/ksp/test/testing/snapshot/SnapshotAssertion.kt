package com.example.ksppluginsetup.ksp.testing.snapshot

import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.core.test.TestScope
import io.kotest.core.test.parents
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Facet-based Markdown golden files.
 *
 * Every captured value is a **facet** — there is no privileged "main" content. Each facet becomes a
 * `## <name>` section followed by one fenced block, in declaration order, so a single golden file
 * holds the input, the options, the exit code, the console output and the generated sources
 * together. Reviewing one file then tells the whole story of a scenario.
 *
 * Regenerate with `./gradlew :<project-name>-ksp:test -Dksppluginsetup.snapshot.update=true`
 * (the build file must forward the `-D` flag to the test worker — it does not propagate on its own).
 */
private const val SNAPSHOT_UPDATE_PROPERTY = "ksppluginsetup.snapshot.update"

private val updateSnapshots: Boolean
    get() = System.getProperty(SNAPSHOT_UPDATE_PROPERTY)?.equals("true", ignoreCase = true) == true

private val snapshotRoot: File by lazy {
    val dir = File("src/test/resources/snapshots")
    if (!dir.exists()) dir.mkdirs()
    dir
}

/**
 * Map a dotted snapshot [name] to its golden path. The **first** `.` is the test-class / test-case
 * boundary and becomes a directory separator; later dots stay in the file name so variant suffixes
 * survive:
 *
 * - `"GreetingSnapshotTest.basic"` → `GreetingSnapshotTest/basic.md`
 * - `"GreetingInvalidUsageTest.onInterface.output"` → `GreetingInvalidUsageTest/onInterface.output.md`
 */
private fun snapshotRelativePath(name: String): String = "${name.replaceFirst(".", "/")}.md"

internal fun assertMatchesSnapshot(
    name: String,
    block: SnapshotFacetBuilder.() -> Unit,
) {
    val facets = SnapshotFacetBuilderImpl().apply(block).build()
    require(facets.isNotEmpty()) {
        "assertMatchesSnapshot(\"$name\") requires at least one facet inside its block."
    }

    val file = File(snapshotRoot, snapshotRelativePath(name))
    val actual = renderFacets(facets)

    if (!file.exists()) {
        if (updateSnapshots) {
            file.parentFile.mkdirs()
            file.writeText(actual, Charsets.UTF_8)
            return
        }
        fail(
            "Snapshot file not found: ${file.path}\n" +
                "Run with -D$SNAPSHOT_UPDATE_PROPERTY=true to create it.\n" +
                "Actual content:\n" + actual,
        )
    }

    val expected = file.readText(Charsets.UTF_8)
    if (expected != actual) {
        if (updateSnapshots) {
            file.writeText(actual, Charsets.UTF_8)
            return
        }
        withClue(
            "Snapshot mismatch for ${file.path}\n" +
                "Run with -D$SNAPSHOT_UPDATE_PROPERTY=true to update.",
        ) {
            actual shouldBe expected
        }
    }
}

/**
 * [TestScope] convenience: derive the golden path from the running test, so a caller inside a
 * `"case" { }` block need not repeat the name. Uses `testCase.name.name` (the string), never the
 * `TestName` object whose `toString()` would leak into the file name.
 */
internal fun TestScope.assertMatchesSnapshot(
    nameSuffix: String? = null,
    block: SnapshotFacetBuilder.() -> Unit,
) {
    val specName =
        "${testCase.spec::class.simpleName ?: "UnknownSpec"}/" +
            testCase.parents().joinToString("/") { it.name.name }
    assertMatchesSnapshot(
        name = "$specName/${testCase.name.name}${nameSuffix?.let { "/$it" } ?: ""}",
        block = block,
    )
}

internal interface SnapshotFacetBuilder {
    /** Add a facet fenced as `kt`. */
    infix fun String.facetOf(content: String)

    /** Add a facet with an explicit fence language (`"text"`, `"properties"`, …). */
    fun facet(
        name: String,
        content: String,
        lang: String = "kt",
    )
}

private class SnapshotFacetBuilderImpl : SnapshotFacetBuilder {
    private val facets = mutableListOf<Facet>()

    override infix fun String.facetOf(content: String) {
        facets += Facet(this, content, "kt")
    }

    override fun facet(
        name: String,
        content: String,
        lang: String,
    ) {
        facets += Facet(name, content, lang)
    }

    fun build(): List<Facet> = facets.toList()
}

private data class Facet(
    val name: String,
    val content: String,
    val lang: String,
)

private fun renderFacets(facets: List<Facet>): String =
    buildString {
        for ((index, facet) in facets.withIndex()) {
            if (index > 0) append('\n')
            append("## ").append(facet.name).append("\n\n")
            append(renderFencedBlock(facet.content, facet.lang)).append('\n')
        }
    }

/**
 * Generated KDoc often contains its own ` ```kt ` example, so the outer fence is one backtick longer
 * than the longest run inside the body (minimum 3). Comparison is whole-file, headings and fences
 * included, so hand-editing a fence breaks the test rather than silently passing.
 */
private fun renderFencedBlock(
    content: String,
    lang: String,
): String {
    val trimmed = content.trimEnd()
    val longestInternalRun = Regex("`+").findAll(trimmed).maxOfOrNull { it.value.length } ?: 0
    val fence = "`".repeat(maxOf(3, longestInternalRun + 1))
    return buildString {
        append(fence).append(lang).append('\n')
        append(trimmed).append('\n')
        append(fence)
    }
}
