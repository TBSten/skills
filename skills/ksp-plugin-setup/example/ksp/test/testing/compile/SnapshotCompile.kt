package com.example.ksppluginsetup.ksp.testing.compile

import com.example.ksppluginsetup.ksp.options.ExampleOptions
import com.example.ksppluginsetup.ksp.testing.snapshot.SnapshotFacetBuilder
import com.example.ksppluginsetup.ksp.testing.snapshot.assertMatchesSnapshot
import com.squareup.kotlinpoet.FileSpec
import io.kotest.core.test.TestScope

/**
 * Compile [inputs] with [options] and pin the whole outcome in one golden file.
 *
 * The fixed facet set is the point: input, options, exit code, console output and generated sources
 * always appear, in the same order, for every scenario — including diagnostics, where the "generated
 * sources" facet being empty and the exit code being `COMPILATION_ERROR` is itself the assertion.
 */
internal inline fun TestScope.runCompileSnapshotTest(
    inputs: List<FileSpec>,
    options: ExampleOptions,
    crossinline assertions: (CompilationResult) -> Unit = { },
    crossinline additionalFacets: SnapshotFacetBuilder.(CompilationResult) -> Unit = { },
): CompilationResult {
    val result =
        compileWithProcessor(options = options.toKspArgs()) {
            inputs.forEach { input -> "${input.name}.kt" source input.toString() }
        }

    assertMatchesSnapshot {
        inputs.forEach { input -> "Input:${input.name}" facetOf input.toString() }
        facet("KSP options", options.toKspConfigString(), lang = "kt")
        facet("Output:ExitCode", result.exitCode.name, lang = "text")
        facet("Output:Console", result.normalizedCompilerOutput(), lang = "text")
        "Output:Generated sources" facetOf result.generatedSourceText()
        additionalFacets(result)
    }

    assertions(result)

    return result
}

/** Single-input overload. */
internal inline fun TestScope.runCompileSnapshotTest(
    input: FileSpec,
    options: ExampleOptions,
    crossinline assertions: (CompilationResult) -> Unit = { },
    crossinline additionalFacets: SnapshotFacetBuilder.(CompilationResult) -> Unit = { },
): CompilationResult =
    runCompileSnapshotTest(
        inputs = listOf(input),
        options = options,
        assertions = assertions,
        additionalFacets = additionalFacets,
    )

/**
 * Options → KSP arg map. Driven by [ExampleOptions.properties], so adding an option is picked up
 * here (and in every snapshot) without touching this function.
 */
internal fun ExampleOptions.toKspArgs(): Map<String, String> =
    ExampleOptions.properties.associate { property ->
        val value = property.get(this)
        "ksppluginsetup.${property.name}" to if (value is Enum<*>) value.name else value.toString()
    }

/** The same options rendered as the `ksp { arg(...) }` block a user would write — a readable facet. */
internal fun ExampleOptions.toKspConfigString(): String =
    buildString {
        appendLine("ksp {")
        toKspArgs().forEach { (key, value) -> appendLine("    arg(\"$key\", \"$value\")") }
        append("}")
    }
