package com.example.ksppluginsetup.ksp.feature

import com.example.ksppluginsetup.ksp.testing.konsist.COMPOSITION_ROOT_TYPES
import com.example.ksppluginsetup.ksp.testing.konsist.FEATURE_PACKAGE
import com.example.ksppluginsetup.ksp.testing.konsist.importsFrom
import com.example.ksppluginsetup.ksp.testing.konsist.inLayer
import com.example.ksppluginsetup.ksp.testing.konsist.kspMain
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

/**
 * Feature-layer boundaries. Each `feature.<name>` package is a per-annotation entry point
 * (discover → validate → call core); it may depend on `core`, `options`, `util` and `ProcessContext`,
 * but never on another `feature.<name>` nor on the composition root.
 *
 * Sub-contexts are generated per feature package so a violation names the offending feature directly.
 */
internal class ArchTest :
    FreeSpec({
        val featurePackages =
            kspMain
                .filter { it.inLayer(FEATURE_PACKAGE) }
                .groupBy { it.packagee?.name.orEmpty() }
                .toSortedMap()

        "the scope actually contains feature packages" {
            withClue("no feature package was detected — the Konsist scope is misconfigured") {
                featurePackages.isNotEmpty() shouldBe true
            }
        }

        "every file lives in feature.<name> (nothing directly under feature/, no deeper nesting)" {
            kspMain
                .filter { it.inLayer(FEATURE_PACKAGE) }
                .assertTrue { file ->
                    val packageName = file.packagee?.name.orEmpty()
                    packageName.startsWith("$FEATURE_PACKAGE.") &&
                        !packageName.removePrefix("$FEATURE_PACKAGE.").contains('.')
                }
        }

        featurePackages.forEach { (packageName, files) ->
            packageName.substringAfterLast('.') - {
                "does not reference another feature" {
                    // Wanting to share code between two features is the signal to move it down into
                    // core, not to import sideways.
                    files.assertFalse { file ->
                        file.imports.any { import ->
                            import.name.startsWith("$FEATURE_PACKAGE.") &&
                                !import.name.startsWith("$packageName.")
                        }
                    }
                }

                "does not depend on the composition root (ProcessContext is the only exception)" {
                    files.assertFalse { file -> file.importsFrom(*COMPOSITION_ROOT_TYPES) }
                }

                "exposes context(ProcessContext) internal fun processXxx(): List<KSAnnotated>" {
                    val entryPoints =
                        files
                            .flatMap { it.functions(includeNested = false, includeLocal = false) }
                            .filter { it.name.startsWith("process") }

                    withClue("no top-level process* function is exposed") {
                        entryPoints.isNotEmpty() shouldBe true
                    }

                    entryPoints.forEach { entryPoint ->
                        withClue("entry point '${entryPoint.name}' should be internal") {
                            entryPoint.hasInternalModifier shouldBe true
                        }
                        withClue(
                            "entry point '${entryPoint.name}' should return List<KSAnnotated> " +
                                "(actual: ${entryPoint.returnType?.sourceType})",
                        ) {
                            entryPoint.returnType?.sourceType shouldBe "List<KSAnnotated>"
                        }
                        // Konsist does not model context parameters, so inspect the signature text —
                        // everything before the body's `{`, so a `context(` inside a string literal or
                        // comment in the body cannot make this pass spuriously.
                        val signature = entryPoint.text.substringBefore('{').trim()
                        withClue(
                            "entry point '${entryPoint.name}' should declare context(ProcessContext) " +
                                "(declaration: $signature)",
                        ) {
                            signature.contains("context(") shouldBe true
                            signature.contains("ProcessContext") shouldBe true
                        }
                    }
                }
            }
        }
    })
