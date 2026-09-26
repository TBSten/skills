package com.example.intellijplugindev

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Keeps the Analysis API channel alive until a feature needs it: K2 analysis runs inside a
 * BasePlatformTestCase and resolves a type.
 *
 * TODO(CUSTOMIZE): replace with a test of a real feature once the plugin reads Kotlin source.
 */
@OptIn(KaAllowAnalysisOnEdt::class)
internal class AnalysisApiSmokeTest : AnalysisTestBase() {

    fun `test the Analysis API resolves the return type of a function`() {
        val file = myFixture.configureByText("Sample.kt", SAMPLE_SRC) as? KtFile
            ?: error("Sample.kt was not parsed as a Kotlin file")
        val function = file.declarations.filterIsInstance<KtNamedFunction>().single { it.name == "greeting" }

        val returnType = runReadActionBlocking {
            allowAnalysisOnEdt {
                analyze(function) {
                    (function.symbol.returnType as? KaClassType)?.classId?.asFqNameString()
                }
            }
        }

        assertEquals("kotlin.String", returnType)
    }

    companion object {
        // The return type is inferred, so the test fails unless the Analysis API really resolves it.
        private val SAMPLE_SRC = """
            package sample

            fun greeting(name: String) = "Hello, " + name
        """.trimIndent()
    }
}
