package com.example.ksppluginsetup.ksp.core

import com.example.ksppluginsetup.ksp.testing.konsist.COMPOSITION_ROOT_TYPES
import com.example.ksppluginsetup.ksp.testing.konsist.CORE_PACKAGE
import com.example.ksppluginsetup.ksp.testing.konsist.CORE_SUBPACKAGES
import com.example.ksppluginsetup.ksp.testing.konsist.FEATURE_PACKAGE
import com.example.ksppluginsetup.ksp.testing.konsist.KSP_API_PACKAGE
import com.example.ksppluginsetup.ksp.testing.konsist.OPTIONS_PACKAGE
import com.example.ksppluginsetup.ksp.testing.konsist.PROCESS_CONTEXT_TYPE
import com.example.ksppluginsetup.ksp.testing.konsist.PROJECT_ROOT
import com.example.ksppluginsetup.ksp.testing.konsist.UTIL_PACKAGE
import com.example.ksppluginsetup.ksp.testing.konsist.importsFrom
import com.example.ksppluginsetup.ksp.testing.konsist.inLayer
import com.example.ksppluginsetup.ksp.testing.konsist.kspMain
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FreeSpec

/**
 * The lower half of `feature → core → util`:
 *
 * - `core` — project-specific generation logic. May use `util` and `options`; must not reach up into
 *   `feature` or the root infra (it takes a narrowed `context(...)` instead of `ProcessContext`).
 * - `core/error` — a leaf that imports nothing but `util`, which is what lets `options` throw from it
 *   without creating a cycle.
 * - `util` — generic helpers, split in two: `util` (top level) is Kotlin-only, and KSP-flavoured
 *   helpers go in `util.ksp`.
 */
internal class ArchTest :
    FreeSpec({
        "core layer" - {
            "does not depend on feature" {
                kspMain
                    .filter { it.inLayer(CORE_PACKAGE) }
                    .assertFalse { file -> file.importsFrom("$FEATURE_PACKAGE.") }
            }

            "does not depend on root infra (ProcessContext / SymbolProcessor / Provider)" {
                kspMain
                    .filter { it.inLayer(CORE_PACKAGE) }
                    .assertFalse { file -> file.importsFrom(PROCESS_CONTEXT_TYPE, *COMPOSITION_ROOT_TYPES) }
            }

            "lives only in the approved sub-packages (nothing directly under core/)" {
                kspMain
                    .filter { it.inLayer(CORE_PACKAGE) }
                    .assertTrue { file -> file.packagee?.name in CORE_SUBPACKAGES }
            }

            "core/error stays a leaf (imports util only)" {
                kspMain
                    .filter { it.packagee?.name == "$CORE_PACKAGE.error" }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.startsWith("$PROJECT_ROOT.") && !import.name.startsWith("$UTIL_PACKAGE.")
                        } || file.importsFrom("$KSP_API_PACKAGE.")
                    }
            }
        }

        "options layer" - {
            "depends only on util and core/error" {
                kspMain
                    .filter { it.inLayer(OPTIONS_PACKAGE) }
                    .assertFalse { file ->
                        file.importsFrom("$FEATURE_PACKAGE.", PROCESS_CONTEXT_TYPE, *COMPOSITION_ROOT_TYPES) ||
                            file.imports.any { import ->
                                import.name.startsWith("$CORE_PACKAGE.") &&
                                    !import.name.startsWith("$CORE_PACKAGE.error.")
                            }
                    }
            }
        }

        "util layer" - {
            "does not depend on core or feature" {
                kspMain
                    .filter { it.inLayer(UTIL_PACKAGE) }
                    .assertFalse { file -> file.importsFrom("$CORE_PACKAGE.", "$FEATURE_PACKAGE.") }
            }

            "references no project-specific type (only its own util package)" {
                // Any other project import would make the helper project-specific and therefore not
                // a generic, reusable util.
                kspMain
                    .filter { it.inLayer(UTIL_PACKAGE) }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.startsWith("$PROJECT_ROOT.") && !import.name.startsWith("$UTIL_PACKAGE.")
                        }
                    }
            }

            "top level (excluding util.ksp) does not depend on the KSP API" {
                kspMain
                    .filter { it.packagee?.name == UTIL_PACKAGE }
                    .assertFalse { file -> file.importsFrom("$KSP_API_PACKAGE.") }
            }
        }
    })
