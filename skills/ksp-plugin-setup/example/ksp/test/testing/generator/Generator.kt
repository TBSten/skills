package com.example.ksppluginsetup.ksp.testing.generator

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary

/**
 * A test-input source with two complementary sides:
 *
 * - [representativeValues] — a small, deterministic, ordered set used by snapshot / example tests.
 *   Must never contain randomness, or goldens churn.
 * - [arb] — a kotest [Arb] for property-based tests over the same space.
 *
 * Keeping both on one type is what lets a snapshot suite and a PBT suite share exactly one
 * definition of "the interesting inputs for this axis".
 */
internal interface Generator<Value> {
    fun representativeValues(): Sequence<GeneratorValue<Value>>

    fun arb(): Arb<Value>

    companion object
}

/** One representative value, optionally labelled. The label becomes the test / golden file name. */
internal data class GeneratorValue<Value>(
    val label: String? = null,
    val value: Value,
)

/**
 * DSL entry point:
 * ```kt
 * val prefixes = generator {
 *     "Default" case "greet"
 *     "short" case "hi"
 *     Arb.string()          // the block's return value is the Arb
 * }
 * ```
 */
internal fun <Value> generator(builder: GeneratorBuilder<Value>.() -> Arb<Value>): Generator<Value> {
    val scope = GeneratorBuilderImpl<Value>()
    val arb = scope.builder()
    return object : Generator<Value> {
        override fun representativeValues(): Sequence<GeneratorValue<Value>> = scope.collected.toList().asSequence()

        override fun arb(): Arb<Value> = arb
    }
}

internal interface GeneratorBuilder<Value> {
    fun case(value: Value)

    infix fun String.case(value: Value)
}

private class GeneratorBuilderImpl<Value> : GeneratorBuilder<Value> {
    val collected = mutableListOf<GeneratorValue<Value>>()

    override fun case(value: Value) {
        collected += GeneratorValue(label = null, value = value)
    }

    override infix fun String.case(value: Value) {
        collected += GeneratorValue(label = this, value = value)
    }
}

internal fun <T, R> Generator<T>.map(transform: (T) -> R): Generator<R> =
    object : Generator<R> {
        override fun representativeValues(): Sequence<GeneratorValue<R>> =
            this@map.representativeValues().map { GeneratorValue(it.label, transform(it.value)) }

        override fun arb(): Arb<R> = arbitrary { rng -> transform(this@map.arb().sample(rng).value) }
    }

internal fun <T> Generator<T>.mapLabel(transform: (String?) -> String?): Generator<T> =
    object : Generator<T> {
        override fun representativeValues(): Sequence<GeneratorValue<T>> =
            this@mapLabel.representativeValues().map { GeneratorValue(transform(it.label), it.value) }

        override fun arb(): Arb<T> = this@mapLabel.arb()
    }

/**
 * Union of same-typed generators: representatives are concatenated in order; the [Arb] picks a
 * member uniformly. The dual of [cartesian] — a union of value spaces rather than a product.
 */
internal fun <T> List<Generator<T>>.union(): Generator<T> {
    require(isNotEmpty()) { "union requires at least one generator" }
    val generators = this
    return object : Generator<T> {
        override fun representativeValues(): Sequence<GeneratorValue<T>> = generators.asSequence().flatMap { it.representativeValues() }

        override fun arb(): Arb<T> = arbitrary { rng -> generators[rng.random.nextInt(generators.size)].arb().sample(rng).value }
    }
}

/**
 * Builder form of [union] that namespaces each member's labels, so a scenario family shows up as
 * `"exclude/…"` rather than colliding in one flat label space.
 */
internal fun <T> union(build: UnionBuilder<T>.() -> Unit): Generator<T> = UnionBuilderImpl<T>().apply(build).members.union()

internal interface UnionBuilder<T> {
    fun case(generator: Generator<T>)

    /** Prefix every member label with `"<this>/"`, falling back to `"<this>[i]"` when unlabelled. */
    infix fun String.case(generator: Generator<T>)
}

private class UnionBuilderImpl<T> : UnionBuilder<T> {
    val members = mutableListOf<Generator<T>>()

    override fun case(generator: Generator<T>) {
        members += generator
    }

    override infix fun String.case(generator: Generator<T>) {
        val prefix = this
        members +=
            object : Generator<T> {
                override fun representativeValues(): Sequence<GeneratorValue<T>> =
                    generator.representativeValues().mapIndexed { index, value ->
                        GeneratorValue(
                            label = value.label?.let { "$prefix/$it" } ?: "$prefix[$index]",
                            value = value.value,
                        )
                    }

                override fun arb(): Arb<T> = generator.arb()
            }
    }
}

/** Full cartesian product of two generators — this is how a scenario family is crossed with options. */
internal fun <A, B> cartesian(
    a: Generator<A>,
    b: Generator<B>,
    label: (String?, String?) -> String? = { x, y -> listOfNotNull(x, y).joinToString(", ") },
): Generator<Pair<A, B>> =
    object : Generator<Pair<A, B>> {
        override fun representativeValues(): Sequence<GeneratorValue<Pair<A, B>>> =
            sequence {
                val rights = b.representativeValues().toList()
                a.representativeValues().forEach { left ->
                    rights.forEach { right ->
                        yield(GeneratorValue(label(left.label, right.label), left.value to right.value))
                    }
                }
            }

        override fun arb(): Arb<Pair<A, B>> = arbitrary { rng -> a.arb().sample(rng).value to b.arb().sample(rng).value }
    }

/**
 * Replace the derived [representativeValues][Generator.representativeValues] with a hand-picked set
 * while keeping [arb][Generator.arb] untouched.
 *
 * This is the escape hatch that keeps a snapshot matrix affordable: a full option product explodes
 * combinatorially, so pin a handful of representative points for the goldens and let property tests
 * still explore the whole space.
 */
internal fun <T> Generator<T>.withRepresentativeValues(builder: GeneratorBuilder<T>.() -> Unit): Generator<T> {
    val base = this
    return generator {
        builder()
        base.arb()
    }
}
