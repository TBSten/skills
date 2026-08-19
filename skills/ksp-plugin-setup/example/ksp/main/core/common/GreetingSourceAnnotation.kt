package com.example.ksppluginsetup.ksp.core.common

import com.example.ksppluginsetup.DefaultGreetingFunName
import com.example.ksppluginsetup.Greeting
import com.google.devtools.ksp.symbol.KSAnnotation

/**
 * [GenerateSourceAnnotation] implementation for `@Greeting`.
 *
 * Add one file like this per annotation, overriding only what differs — never a `when` over the
 * implementations. Everything not overridden keeps the interface's safe default.
 */
internal class GreetingSourceAnnotation(
    private val annotation: KSAnnotation,
) : GenerateSourceAnnotation {
    override val funName: String?
        get() =
            annotation
                .argumentValue<String>(Greeting::funName.name)
                ?.takeIf { it != DefaultGreetingFunName }

    /** `@Greeting.Exclude` is a real marker on this annotation, so an ineffective one is worth a warning. */
    override val warnsIneffectiveExclude: Boolean get() = true

    /** Per-property rule, handed to the generators as a parameter (see [IsExcluded]). */
    val isExcluded: IsExcluded =
        IsExcluded { property ->
            property.annotations.any { it.shortName.asString() == Greeting.Exclude::class.simpleName }
        }
}

/** `firstOrNull()` over `first()`: a missing argument is a diagnostic, never an exception. */
private inline fun <reified T> KSAnnotation.argumentValue(name: String): T? =
    arguments.firstOrNull { it.name?.asString() == name }?.value as? T
