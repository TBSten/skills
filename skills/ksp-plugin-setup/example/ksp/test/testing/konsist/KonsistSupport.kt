package com.example.ksppluginsetup.ksp.testing.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

/**
 * Shared Konsist scope and layer predicates for the architecture tests.
 *
 * The authoritative dependency table lives in `.claude/rules/ksp-architecture.md`; this file only
 * encodes the package names and reusable predicates the specs share. Change the table → change these
 * specs, in the same commit.
 */

internal const val PROJECT_ROOT = "com.example.ksppluginsetup"
internal const val KSP_ROOT = "$PROJECT_ROOT.ksp"
internal const val UTIL_PACKAGE = "$KSP_ROOT.util"
internal const val CORE_PACKAGE = "$KSP_ROOT.core"
internal const val OPTIONS_PACKAGE = "$KSP_ROOT.options"
internal const val FEATURE_PACKAGE = "$KSP_ROOT.feature"
internal const val KSP_API_PACKAGE = "com.google.devtools.ksp"
internal const val PROCESS_CONTEXT_TYPE = "$KSP_ROOT.ProcessContext"

/** File-length budget. 10–300 lines is the target; anything longer is a responsibility to split. */
internal const val MAX_FILE_LINES = 300

/**
 * Composition-root infra neither `core` nor `feature` may import. (`feature` may still import
 * [PROCESS_CONTEXT_TYPE].) Listed explicitly rather than by prefix so the intent matches the rules
 * document.
 */
internal val COMPOSITION_ROOT_TYPES =
    arrayOf(
        "$KSP_ROOT.ExampleSymbolProcessor",
        "$KSP_ROOT.ExampleSymbolProcessorProvider",
    )

/**
 * Per-file overrides to [MAX_FILE_LINES]. Keep this map tiny and justify every entry in a comment —
 * it is an escape hatch, not the norm.
 */
internal val FILE_LINE_LIMIT_OVERRIDES = mapOf<String, Int>()

/** The only sub-packages `core` may contain — `core/` itself must hold no `.kt` files. */
internal val CORE_SUBPACKAGES =
    setOf(
        "$CORE_PACKAGE.common",
        "$CORE_PACKAGE.greetingFun",
        "$CORE_PACKAGE.error",
    )

/** The only files allowed directly in the root package. */
internal val ROOT_ALLOWED_FILES =
    setOf(
        "ExampleSymbolProcessor.kt",
        "ExampleSymbolProcessorProvider.kt",
        "ProcessContext.kt",
    )

/** The production source set only — the test source set is deliberately excluded. */
internal val kspMain: List<KoFileDeclaration> by lazy {
    Konsist.scopeFromProduction(moduleName = "<project-name>-ksp", sourceSetName = "main").files
}

/** True when this file's package is [layerPackage] or one of its sub-packages. */
internal fun KoFileDeclaration.inLayer(layerPackage: String): Boolean {
    val packageName = packagee?.name ?: return false
    return packageName == layerPackage || packageName.startsWith("$layerPackage.")
}

/** True when this file imports anything whose FQN starts with one of [importPrefixes]. */
internal fun KoFileDeclaration.importsFrom(vararg importPrefixes: String): Boolean =
    imports.any { import -> importPrefixes.any { prefix -> import.name.startsWith(prefix) } }
