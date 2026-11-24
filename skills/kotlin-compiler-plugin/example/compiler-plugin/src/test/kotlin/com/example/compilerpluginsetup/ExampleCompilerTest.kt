package com.example.compilerpluginsetup

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.FunSpec

/**
 * Unit tests with kctfork (in-memory KotlinCompilation).
 *
 * Test categories to grow from here:
 * 1. Happy path — the transformation is applied correctly
 * 2. Error cases — expected compile errors (`ExitCode.COMPILATION_ERROR`)
 * 3. Edge cases — type variations, nesting, multiple parameters, ...
 */
class ExampleCompilerTest : FunSpec({

    fun compile(source: String, dumpIr: Boolean = false): JvmCompilationResult =
        KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Source.kt", source))
            compilerPluginRegistrars = listOf(ExampleRegistrar())
            inheritClassPath = true
            jvmTarget = "21"
            messageOutputStream = System.out
            if (dumpIr) kotlincArguments = listOf("-Xphases-to-dump-after=IrVerification")
        }.compile()

    fun JvmCompilationResult.shouldCompileOk(): JvmCompilationResult {
        if (exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n$messages")
        }
        return this
    }

    /** Loads a top-level property value via the classloader of the compilation result. */
    fun JvmCompilationResult.loadTopLevelField(
        name: String,
        pkg: String? = null,
    ): Any? {
        val className = if (pkg != null) "$pkg.SourceKt" else "SourceKt"
        return classLoader.loadClass(className)
            .getDeclaredField(name)
            .also { it.isAccessible = true }
            .get(null)
    }

    test("プラグインを適用してもコンパイルが成功する") {
        compile(
            """
            package com.example.test
            fun greet(name: String = "world"): String = "Hello, ${'$'}name!"
            val v = greet()
            """.trimIndent(),
        ).shouldCompileOk()
    }

    // TODO: Once the IR transformation is implemented, assert on transformed values:
    //
    // test("変換された値が取得できる") {
    //     val result = compile("...").shouldCompileOk()
    //     result.loadTopLevelField("v", pkg = "com.example.test") shouldBe "expected"
    // }
})
