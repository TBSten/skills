package com.example.ksppluginsetup.ksp.feature.greeting

import com.example.ksppluginsetup.Greeting
import com.example.ksppluginsetup.ksp.ProcessContext
import com.example.ksppluginsetup.ksp.core.common.GreetingSourceAnnotation
import com.example.ksppluginsetup.ksp.core.common.createNewKotlinFile
import com.example.ksppluginsetup.ksp.core.greetingFun.appendGreetingFunction
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotated
import com.example.ksppluginsetup.ksp.util.with
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

/**
 * One feature = one annotation = one file. This file does discovery → validation → a call into
 * `core`, and nothing else. No code assembly lives here (see `.claude/rules/ksp-feature-top-level.md`).
 *
 * The signature shape is enforced by the Konsist feature architecture test:
 * `context(ProcessContext) internal fun processXxx(): List<KSAnnotated>`.
 *
 * The returned list is the symbols that did not `validate()` this round — KSP re-offers them next
 * round once the types they reference exist.
 */
context(processContext: ProcessContext)
internal fun processGreeting(): List<KSAnnotated> {
    val (valid, invalid) =
        processContext.resolver
            .getSymbolsWithAnnotation(Greeting::class.qualifiedName!!)
            .partition { it.validate() }

    valid.forEach { symbol ->
        // Misuse is reported and skipped — never thrown. Throwing would surface as an opaque
        // INTERNAL_ERROR and could leave a half-written file behind.
        val declaration =
            symbol as? KSClassDeclaration
                ?: run {
                    processContext.logger.error(
                        "@Greeting can only be applied to a class or object.\n" +
                            "Solution: move the annotation onto a class declaration.",
                        symbol,
                    )
                    return@forEach
                }

        val annotation =
            declaration.annotations
                .firstOrNull { it.shortName.asString() == Greeting::class.simpleName }
                ?: return@forEach
        val generateSourceAnnotation = GreetingSourceAnnotation(annotation)

        processContext.codeGenerator.createNewKotlinFile(
            dependencies = Dependencies(aggregating = true, declaration.containingFile!!),
            packageName = declaration.packageName,
            fileName = "Greeting__${declaration.simpleName.asString()}",
        ) { out ->
            with(processContext.options, processContext.logger) {
                out.appendGreetingFunction(
                    declaration = declaration,
                    generateSourceAnnotation = generateSourceAnnotation,
                    isExcluded = generateSourceAnnotation.isExcluded,
                )
            }
        }
    }

    return invalid
}
