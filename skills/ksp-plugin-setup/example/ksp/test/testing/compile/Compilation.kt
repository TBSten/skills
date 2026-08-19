@file:OptIn(ExperimentalCompilerApi::class)

package com.example.ksppluginsetup.ksp.testing.compile

import com.example.ksppluginsetup.Greeting
import com.example.ksppluginsetup.ksp.ExampleSymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

/** DSL receiver for multi-file inputs: `"Source.kt" source """..."""`. */
internal class SourcesBuilder {
    private val sources = mutableListOf<SourceFile>()

    infix fun String.source(
        @Language("kotlin") code: String,
    ) {
        sources += SourceFile.kotlin(this, code)
    }

    internal fun build(): List<SourceFile> = sources.toList()
}

/**
 * Compile [block]'s sources with the real Kotlin compiler and the real processor (kctfork).
 *
 * ```kt
 * compileWithProcessor(options = mapOf("ksppluginsetup.greetingStyle" to "polite")) {
 *     "Source.kt" source """
 *         @Greeting
 *         data class User(val name: String)
 *     """.trimIndent()
 * }
 * ```
 */
internal fun compileWithProcessor(
    options: Map<String, String> = emptyMap(),
    block: SourcesBuilder.() -> Unit,
): CompilationResult = runCompilation(SourcesBuilder().apply(block).build(), options)

/** Single-source convenience overload. */
internal fun compileWithProcessor(
    @Language("kotlin") source: String,
    options: Map<String, String> = emptyMap(),
    sourceFileName: String = "Test.kt",
): CompilationResult = compileWithProcessor(options = options) { sourceFileName source source }

private fun runCompilation(
    sources: List<SourceFile>,
    options: Map<String, String>,
): CompilationResult {
    val captured = ByteArrayOutputStream()
    val compilation =
        KotlinCompilation().apply {
            // `inheritClassPath = true` drags the whole test runtime classpath into every
            // compilation and dominates the suite's wall clock. Pin the minimum instead.
            inheritClassPath = false
            classpaths = processorCompilationClasspath
            useKsp2()
            symbolProcessorProviders += ExampleSymbolProcessorProvider()
            if (options.isNotEmpty()) {
                kspProcessorOptions = options.toMutableMap()
            }
            this.sources = sources
            // Tee: keep the output visible in the console AND capture it for the golden file.
            messageOutputStream = TeeOutputStream(System.out, captured)
        }
    return CompilationResult(
        raw = compilation.compile(),
        compilation = compilation,
        compilerOutputBuffer = captured,
    )
}

private val processorCompilationClasspath: List<File> =
    listOf(
        Greeting::class.java, // the runtime module
        Unit::class.java, // kotlin-stdlib
    ).map { it.classpathRoot() }.distinct()

private fun Class<*>.classpathRoot(): File {
    val location =
        checkNotNull(protectionDomain?.codeSource?.location) {
            "Cannot locate the classpath root for $name (codeSource is null)."
        }
    return File(location.toURI())
}

internal data class CompilationResult(
    private val raw: JvmCompilationResult,
    val compilation: KotlinCompilation,
    private val compilerOutputBuffer: ByteArrayOutputStream,
) {
    val exitCode: KotlinCompilation.ExitCode get() = raw.exitCode
    val messages: String get() = raw.messages

    /** Everything the compiler / KSP printed. Normalize before snapshotting (absolute temp paths). */
    val compilerOutput: String get() = compilerOutputBuffer.toString(Charsets.UTF_8)

    fun generatedSources(): List<File> = raw.sourcesGeneratedBySymbolProcessor.toList()

    fun loadGeneratedClass(fqName: String): Class<*> = raw.classLoader.loadClass(fqName)
}

/** All generated files concatenated in a stable order, ready to be one snapshot facet. */
internal fun CompilationResult.generatedSourceText(): String =
    generatedSources()
        .sortedBy { it.name }
        .joinToString(separator = "\n\n// ----- next file -----\n\n") { file ->
            "// file: ${file.name}\n" + file.readText().trimEnd()
        }

/**
 * Compiler / KSP output with machine-specific bits replaced by stable placeholders so it can be
 * committed as a golden file. Stack frames are collapsed on purpose: their contents and the
 * `... NN more` depth shift with every JVM / Gradle / KSP version and with your own line moves, so
 * only the message body is pinned. Assert on a specific frame with a separate `shouldContain`.
 */
internal fun CompilationResult.normalizedCompilerOutput(): String {
    val tmpDir = System.getProperty("java.io.tmpdir").trimEnd('/', '\\')
    return compilerOutput
        .replace(tmpDir, "<TMPDIR>")
        .replace(Regex("Kotlin-Compilation\\d+"), "Kotlin-Compilation<N>")
        .replace(Regex("(?:\\n\\tat [^\\n]+|\\n\\t\\.\\.\\. \\d+ more)+"), "\n\t<stack trace omitted>")
        .trimEnd() + if (compilerOutput.isEmpty()) "" else "\n"
}

private class TeeOutputStream(
    private val a: OutputStream,
    private val b: OutputStream,
) : OutputStream() {
    override fun write(byte: Int) {
        a.write(byte)
        b.write(byte)
    }

    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ) {
        a.write(buffer, offset, length)
        b.write(buffer, offset, length)
    }

    override fun flush() {
        a.flush()
        b.flush()
    }
}
