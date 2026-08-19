package com.example.ksppluginsetup.ksp.core.greetingFun

import com.example.ksppluginsetup.ksp.core.common.GenerateSourceAnnotation
import com.example.ksppluginsetup.ksp.core.common.IsExcluded
import com.example.ksppluginsetup.ksp.options.ExampleOptions
import com.example.ksppluginsetup.ksp.options.GreetingStyle
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Generation is plain string `append` onto an [Appendable] — no KotlinPoet.
 *
 * That holds as long as the output is simple (functions, properties, straightforward signatures). If
 * you start assembling complex generic types, reconsider KotlinPoet; the trade-off is readability of
 * the generator versus correctness of the type rendering.
 *
 * Note the context: `core` receives only the capabilities it needs, never the whole `ProcessContext`
 * (no `resolver`, no `codeGenerator`). That is what keeps `core` independent of the root layer.
 */
context(options: ExampleOptions, logger: KSPLogger)
internal fun Appendable.appendGreetingFunction(
    declaration: KSClassDeclaration,
    generateSourceAnnotation: GenerateSourceAnnotation,
    isExcluded: IsExcluded,
) {
    if (declaration.classKind == ClassKind.OBJECT &&
        generateSourceAnnotation.skipsObjectTarget(options.skipObject)
    ) {
        return
    }

    val simpleName = declaration.simpleName.asString()
    val funName = generateSourceAnnotation.funName ?: "${options.greetingFunPrefix}$simpleName"
    val greeted =
        declaration
            .getAllProperties()
            .filterNot { isExcluded(it) }
            .joinToString(", ") { it.simpleName.asString() }

    // `when` without `else`: adding a GreetingStyle entry becomes a compile error here rather than a
    // silently wrong default at runtime.
    val salutation =
        when (options.greetingStyle) {
            GreetingStyle.plain -> "Hi"
            GreetingStyle.polite -> "Good day"
        }

    if (greeted.isEmpty()) {
        logger.warn("$simpleName has no property to greet; generating a bare greeting.", declaration)
    }

    appendLine("public fun $simpleName.$funName(): String =")
    appendLine("    \"$salutation, $simpleName($greeted)\"")
    appendLine()
}
