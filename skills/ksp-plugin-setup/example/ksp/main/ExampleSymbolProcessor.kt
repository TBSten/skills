package com.example.ksppluginsetup.ksp

import com.example.ksppluginsetup.ksp.core.error.InvalidExampleOptionException
import com.example.ksppluginsetup.ksp.feature.greeting.processGreeting
import com.example.ksppluginsetup.ksp.options.ExampleOptions
import com.example.ksppluginsetup.ksp.options.toExampleOptions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Composition root. Owns option parsing and dispatches to each feature — and nothing else. No
 * generation logic and no per-annotation handling live here (see `.claude/rules/ksp-top-level.md`).
 */
internal class ExampleSymbolProcessor(
    private val rawOptions: Map<String, String>,
    internal val codeGenerator: CodeGenerator,
    internal val logger: KSPLogger,
) : SymbolProcessor {
    // Parsed lazily so an invalid option value surfaces as a clean COMPILATION_ERROR from process()
    // (where the logger is available) rather than as a constructor crash, which KSP reports as an
    // opaque INTERNAL_ERROR. Backing field is set once on first successful parse.
    private var parsedOptions: ExampleOptions? = null
    internal val options: ExampleOptions
        get() = parsedOptions ?: rawOptions.toExampleOptions().also { parsedOptions = it }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // An invalid option is a build-script mistake with no source location, so it is reported
        // without a KSNode and the whole round is skipped — never a partially generated file.
        val parsed =
            try {
                options
            } catch (e: InvalidExampleOptionException) {
                logger.error(e.message.orEmpty())
                return emptyList()
            }

        // The per-round infrastructure every feature shares. `with(ctx)` supplies the context
        // parameter each `processXxx` declares as `context(ctx: ProcessContext)`.
        val processContext = ProcessContext(resolver, parsed, codeGenerator, logger)

        return with(processContext) {
            buildList {
                addAll(processGreeting())
                // Add one line per feature. Nothing else changes in this file.
            }
        }
    }
}
