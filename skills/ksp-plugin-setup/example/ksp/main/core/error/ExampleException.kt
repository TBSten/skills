package com.example.ksppluginsetup.ksp.core.error

import com.example.ksppluginsetup.ksp.util.appendLines

/**
 * Exception hierarchy: one abstract base, a misuse branch and an option branch derived from it, and
 * a separate `Unknown` for anything unforeseen.
 *
 * This package is a **leaf** — it imports nothing but `util`. That is what lets `options/` throw
 * from it without creating a cycle (`options` is otherwise referenced by `core` / `feature` / root).
 *
 * Note these exceptions are for *internal* control flow and for option parsing, which has no source
 * location. A user's misuse of an annotation is NOT thrown: it is reported with
 * `logger.error(message, ksNode)` followed immediately by a `return`, because throwing turns a clean
 * COMPILATION_ERROR into an INTERNAL_ERROR and can leave a half-written file behind.
 */
internal abstract class ExampleException(
    message: String,
    solution: String? = null,
    cause: Throwable? = null,
) : Exception(
        buildString {
            appendLine(message)

            if (solution != null) {
                appendLine()
                appendLine("Solution: ")
                solution.lineSequence().forEach {
                    appendLine("  $it")
                }
            }
        },
        cause,
    )

internal open class InvalidExampleUsageException(
    message: String,
    solution: String?,
    cause: Throwable? = null,
) : ExampleException(
        message = "Invalid usage: $message",
        solution = solution,
        cause = cause,
    )

internal class InvalidExampleOptionException(
    message: String,
    solution: String?,
    cause: Throwable? = null,
) : InvalidExampleUsageException(
        message = "Invalid option: $message",
        solution = solution,
        cause = cause,
    )

/** Anything the processor did not anticipate — the message always carries a report-it link. */
internal class UnknownExampleException(
    message: String? = null,
    solution: String? = null,
    cause: Throwable? = null,
) : ExampleException(
        // `+ null` would render the literal "null", so the suffix defaults to "" instead.
        message = "Unexpected error" + (message?.let { ": $it" } ?: ""),
        solution = solution ?: reportToGithub(),
        cause = cause,
    )

internal fun reportToGithub(vararg with: String): String =
    buildString {
        appendLines(
            "Please report this issue at:",
            "",
            "    https://github.com/<owner>/<repo>/issues",
            "",
        )

        if (with.isNotEmpty()) {
            appendLines("  and report problems with:")
            with.forEach { appendLines("    - $it") }
            appendLine()
        }
    }
