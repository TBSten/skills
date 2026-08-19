package com.example.ksppluginsetup.ksp.options

import com.example.ksppluginsetup.ksp.core.error.InvalidExampleOptionException
import com.example.ksppluginsetup.ksp.util.lines
import kotlin.reflect.KProperty1

/**
 * Every KSP option in one data class — the single source of truth for names, types and defaults.
 *
 * [properties] exists so tests (option → `ksp { arg(...) }` rendering, matrix generators) iterate the
 * real property list instead of a hand-maintained copy: add an option here and the tests follow.
 */
internal data class ExampleOptions(
    val greetingFunPrefix: String,
    val greetingStyle: GreetingStyle,
    val skipObject: Boolean,
) {
    companion object {
        val default: ExampleOptions =
            ExampleOptions(
                greetingFunPrefix = "greet",
                greetingStyle = GreetingStyle.default,
                skipObject = false,
            )

        val properties: List<KProperty1<ExampleOptions, *>> =
            listOf(
                ExampleOptions::greetingFunPrefix,
                ExampleOptions::greetingStyle,
                ExampleOptions::skipObject,
            )
    }
}

/**
 * Serialized names are the enum entry names, so the value written in `ksp { arg(...) }` and the
 * value shown in a diagnostic are the same string. Backticked kebab-case keeps the build script
 * readable.
 */
@Suppress("EnumEntryName", "ktlint:standard:enum-entry-name-case")
internal enum class GreetingStyle {
    `plain`,
    `polite`,
    ;

    companion object {
        val default: GreetingStyle = `plain`
    }
}

/**
 * Parse the raw KSP arguments. Called lazily from `process()` — never from a constructor — so a bad
 * value becomes a reported COMPILATION_ERROR instead of an INTERNAL_ERROR.
 */
internal fun Map<String, String>.toExampleOptions(): ExampleOptions =
    ExampleOptions(
        greetingFunPrefix = this["ksppluginsetup.greetingFunPrefix"] ?: ExampleOptions.default.greetingFunPrefix,
        greetingStyle =
            this["ksppluginsetup.greetingStyle"]?.let { rawValue ->
                try {
                    GreetingStyle.valueOf(rawValue)
                } catch (e: IllegalArgumentException) {
                    invalidGreetingStyleError(actualValue = rawValue, cause = e)
                }
            } ?: ExampleOptions.default.greetingStyle,
        // Lenient boolean: only an explicit "true" enables it, so a typo never fails the build.
        skipObject = this["ksppluginsetup.skipObject"]?.lowercase() == "true",
    )

private fun invalidGreetingStyleError(
    actualValue: String?,
    cause: IllegalArgumentException,
): Nothing =
    throw InvalidExampleOptionException(
        message =
            lines(
                "Invalid ksp.arg[\"ksppluginsetup.greetingStyle\"] = $actualValue.",
                "It must be one of ${GreetingStyle.entries.joinToString(", ")}.",
            ),
        // Every diagnostic states how to fix it, not only what is wrong.
        solution =
            lines(
                "Set one of the following for ksp.arg:",
                "",
                *GreetingStyle.entries.map { "  - \"${it.name}\"" }.toTypedArray(),
            ),
        cause = cause,
    )
