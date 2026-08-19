package com.example.ksppluginsetup.ksp.feature.greeting

import com.example.ksppluginsetup.ksp.testing.compile.compileWithProcessor
import com.example.ksppluginsetup.ksp.testing.compile.normalizedCompilerOutput
import com.example.ksppluginsetup.ksp.testing.snapshot.assertMatchesSnapshot
import com.tschuchort.compiletesting.KotlinCompilation
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe

/**
 * Diagnostics are golden-tested too: the **error message text itself** is the contract, not just the
 * fact that compilation failed. Pinning it means a message losing its "Solution:" line, or drifting
 * into jargon, shows up as a reviewable diff.
 *
 * The exit code is asserted alongside the golden — a message that is right while the build wrongly
 * succeeds would otherwise pass.
 */
internal class GreetingInvalidUsageTest :
    FreeSpec({
        "an invalid option value is reported without a source location" {
            val source =
                """
                import com.example.ksppluginsetup.Greeting

                @Greeting
                data class User(val name: String)
                """.trimIndent()

            val result =
                compileWithProcessor(
                    source = source,
                    options = mapOf("ksppluginsetup.greetingStyle" to "shouty"),
                )

            result.exitCode shouldNotBe KotlinCompilation.ExitCode.OK

            assertMatchesSnapshot {
                "Input" facetOf source
                facet("Output:ExitCode", result.exitCode.name, lang = "text")
                facet("Output:Console", result.normalizedCompilerOutput(), lang = "text")
            }
        }
    })
