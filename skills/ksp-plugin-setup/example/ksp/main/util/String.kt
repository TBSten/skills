package com.example.ksppluginsetup.ksp.util

/**
 * `util/` (top level) holds helpers that would work in any project: Kotlin stdlib only, no KSP API,
 * no project-specific types. Helpers that touch the KSP API belong in `util/ksp/` instead.
 *
 * The Konsist architecture test enforces both halves of that rule.
 */
internal fun lines(vararg lines: String): String = lines.joinToString(separator = "\n")

internal fun Appendable.appendLines(vararg lines: String) {
    lines.forEach { appendLine(it) }
}
