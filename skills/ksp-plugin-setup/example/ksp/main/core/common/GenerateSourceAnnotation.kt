package com.example.ksppluginsetup.ksp.core.common

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Identifies which annotation triggered a generation and carries that annotation's generation rules.
 *
 * **Deliberately not `sealed`.** Rules are resolved by polymorphic dispatch, never by an exhaustive
 * `when` over the implementations — so adding an annotation means adding one implementation file and
 * overriding only the rules that differ, with no branch edited anywhere else. Each rule has a "does
 * nothing special" default, so a new implementation starts correct.
 *
 * Rules decided **once per generated function** belong here. Rules decided **per property** do NOT:
 * they are standalone function types ([IsExcluded]) that the generators take as ordinary parameters,
 * so a caller that is not driving generation from an annotation can pass a plain lambda.
 */
internal interface GenerateSourceAnnotation {
    /** The `funName` written on the annotation, or `null` when the user did not specify one. */
    val funName: String?

    /** Whether an `object` declaration is skipped. Default: follow the project-wide option. */
    fun skipsObjectTarget(skipObjectOption: Boolean): Boolean = skipObjectOption

    /** Whether an `@Exclude` that matches nothing deserves a warning. Default: no. */
    val warnsIneffectiveExclude: Boolean get() = false
}

/**
 * Per-property rule, passed to the generators as a normal parameter rather than read off
 * [GenerateSourceAnnotation]. An annotation that has `@Exclude` semantics exposes one as a property
 * of the same name; a test or a non-annotation caller passes `IsExcluded { false }`.
 */
internal fun interface IsExcluded {
    operator fun invoke(property: KSPropertyDeclaration): Boolean
}
