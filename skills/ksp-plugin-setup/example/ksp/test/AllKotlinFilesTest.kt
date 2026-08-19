package com.example.ksppluginsetup.ksp

import com.example.ksppluginsetup.ksp.testing.konsist.FILE_LINE_LIMIT_OVERRIDES
import com.example.ksppluginsetup.ksp.testing.konsist.KSP_ROOT
import com.example.ksppluginsetup.ksp.testing.konsist.MAX_FILE_LINES
import com.example.ksppluginsetup.ksp.testing.konsist.ROOT_ALLOWED_FILES
import com.example.ksppluginsetup.ksp.testing.konsist.kspMain
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FreeSpec

/**
 * Module-wide guardrails that apply to every production file regardless of layer. Layer-specific
 * boundaries live next to the layer they constrain ([com.example.ksppluginsetup.ksp.feature.ArchTest],
 * [com.example.ksppluginsetup.ksp.core.ArchTest]).
 *
 * Checks are import-based, which is why the project convention is to import referenced symbols
 * rather than use fully-qualified inline references — an inline FQN would slip past these rules.
 */
internal class AllKotlinFilesTest :
    FreeSpec({
        "root layer (composition root)" - {
            "holds only the approved infra files" {
                // The root package is the composition root. Generation logic, helpers and exceptions
                // (which belong in core/error) must never be added here.
                kspMain
                    .filter { it.packagee?.name == KSP_ROOT }
                    .assertTrue { file -> file.nameWithExtension in ROOT_ALLOWED_FILES }
            }
        }

        "every file" - {
            "stays within $MAX_FILE_LINES lines (FILE_LINE_LIMIT_OVERRIDES may raise it per file)" {
                kspMain.assertFalse { file ->
                    val limit = FILE_LINE_LIMIT_OVERRIDES[file.nameWithExtension] ?: MAX_FILE_LINES
                    file.text.lines().size > limit
                }
            }
        }
    })
