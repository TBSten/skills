package com.example.ksppluginsetup

/**
 * Example generation annotation. The runtime module holds **declarations only** — no logic — so it
 * can be published for every Kotlin target.
 *
 * Conventions worth keeping when you add more annotations:
 * - `@Retention(SOURCE)` on every processor-only annotation (it is never needed at runtime).
 * - A uniform trailing surface (`funName`, visibility, KDoc, …) in the same order on every
 *   generation annotation, always with defaults, so adding a parameter never changes existing
 *   behaviour.
 * - Auxiliary markers nest inside their parent (`@Greeting.Exclude`) instead of becoming a shared
 *   top-level type, so `@Target` can be narrowed per parent and the usage self-documents.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class Greeting(
    /**
     * Name of the generated function. Defaults to the [DefaultGreetingFunName] sentinel so the
     * processor can tell "not specified" from an explicit name.
     *
     * Tokens are `const val String` sentinels rather than an enum so a call site can concatenate
     * them (`GreetingFunNameToken.Prefix + "Hello"`) and stay a compile-time constant, which an
     * annotation argument requires. Substitution happens in the processor.
     */
    val funName: String = DefaultGreetingFunName,
) {
    /** Marks a property the generated greeting should skip. */
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
    public annotation class Exclude
}

public const val DefaultGreetingFunName: String = "{{ksppluginsetup:DefaultGreetingFunName}}"

/** Tokens usable inside [Greeting.funName]; replaced by the processor. */
public object GreetingFunNameToken {
    /** Expands to the annotated declaration's simple name. */
    public const val SimpleName: String = "{{ksppluginsetup:SimpleName}}"
}
