package com.example.ksppluginsetup.ksp.testing.generator

import com.example.ksppluginsetup.ksp.options.ExampleOptions
import com.example.ksppluginsetup.ksp.options.GreetingStyle
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.of

/**
 * The option axis, with the two sides deliberately different:
 *
 * - [Generator.arb] samples the **full** product of every axis (for property tests).
 * - [Generator.representativeValues] is narrowed to a handful of points centred on
 *   [ExampleOptions.default] (for snapshots). The full product here would be 2 × 2 × 2 = 8, and it
 *   multiplies by every scenario in every family — real projects hit hundreds of compilations fast.
 *
 * When adding an option, ask whether it can change generated output. If it cannot, pin it to the
 * default here and cover it with one targeted test instead of multiplying the whole matrix.
 */
internal fun Generator.Companion.validExampleOptions(): Generator<ExampleOptions> =
    fullProduct().withRepresentativeValues {
        listOf(
            ExampleOptions.default,
            ExampleOptions.default.copy(greetingFunPrefix = "hello", greetingStyle = GreetingStyle.polite),
            ExampleOptions.default.copy(skipObject = true),
        ).distinct().forEach { options -> optionsLabel(options) case options }
    }

/** The single `Default` point, for a family no option can move. */
internal fun Generator.Companion.defaultExampleOptionsOnly(): Generator<ExampleOptions> =
    validExampleOptions().withRepresentativeValues {
        optionsLabel(ExampleOptions.default) case ExampleOptions.default
    }

private fun fullProduct(): Generator<ExampleOptions> =
    generator {
        case(ExampleOptions.default)
        Arb.bind(
            Arb.of("greet", "hello"),
            Arb.of(*GreetingStyle.entries.toTypedArray()),
            Arb.boolean(),
        ) { prefix, style, skipObject ->
            ExampleOptions(greetingFunPrefix = prefix, greetingStyle = style, skipObject = skipObject)
        }
    }

/** Label only what differs from the default, so golden file names stay short and diff-readable. */
private fun optionsLabel(options: ExampleOptions): String {
    val default = ExampleOptions.default
    val parts =
        buildList {
            if (options.greetingFunPrefix != default.greetingFunPrefix) add("prefix=${options.greetingFunPrefix}")
            if (options.greetingStyle != default.greetingStyle) add("style=${options.greetingStyle.name}")
            if (options.skipObject != default.skipObject) add("skipObject=${options.skipObject}")
        }
    return if (parts.isEmpty()) "Default" else parts.joinToString(", ", prefix = "(", postfix = ")")
}
