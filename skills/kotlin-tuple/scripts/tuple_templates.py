"""Kotlin source templates for the kotlin-tuple generator.

Imported by generate.py (same directory). Byte-identical to example/ at
--package com.example.tuple --max 20 -- guarded by `generate.py --self-test`.
Do NOT edit generated output by hand; fix these templates instead.
"""

# ---------------------------------------------------------------- number words

ORDINALS_1_20 = [
    "first", "second", "third", "fourth", "fifth",
    "sixth", "seventh", "eighth", "ninth", "tenth",
    "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth",
    "sixteenth", "seventeenth", "eighteenth", "nineteenth", "twentieth",
]
CARDINALS_0_20 = [
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight",
    "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
    "sixteen", "seventeen", "eighteen", "nineteen", "twenty",
]
TENS_CARDINAL = {2: "twenty", 3: "thirty", 4: "forty", 5: "fifty",
                 6: "sixty", 7: "seventy", 8: "eighty", 9: "ninety"}
TENS_ORDINAL = {2: "twentieth", 3: "thirtieth", 4: "fortieth", 5: "fiftieth",
                6: "sixtieth", 7: "seventieth", 8: "eightieth", 9: "ninetieth"}


def ordinal(i):
    """1-based ordinal identifier: 1 -> 'first', 21 -> 'twentyFirst' (Kotlin-safe camelCase)."""
    if i <= 20:
        return ORDINALS_1_20[i - 1]
    tens, unit = divmod(i, 10)
    if unit == 0:
        return TENS_ORDINAL[tens]
    unit_ord = ORDINALS_1_20[unit - 1]
    return TENS_CARDINAL[tens] + unit_ord[0].upper() + unit_ord[1:]


def cardinal(n):
    """Cardinal English words for KDoc: 21 -> 'twenty-one'."""
    if n <= 20:
        return CARDINALS_0_20[n]
    tens, unit = divmod(n, 10)
    if unit == 0:
        return TENS_CARDINAL[tens]
    return TENS_CARDINAL[tens] + "-" + CARDINALS_0_20[unit]


def ords(n):
    return [ordinal(i) for i in range(1, n + 1)]


def types(n):
    return ", ".join("A%d" % i for i in range(n))


# ---------------------------------------------------------------- Tuple.kt

def gen_tuple(pkg, max_n):
    parts = ["""/**
 * Type-safe Tuple data classes for Kotlin.
 *
 * Provides [Tuple0] through [Tuple%d] for grouping multiple values with distinct types.
 * [Tuple2] and [Tuple3] are typealiases for [Pair] and [Triple] respectively.
 *
 * @see tupleOf Factory functions to create Tuple instances.
 */
package %s""" % (max_n, pkg)]
    parts.append("""/**
 * A tuple with zero elements.
 */
data object Tuple0 {
    override fun toString(): String = "()"
}""")
    parts.append("""/**
 * A tuple with one element.
 */
data class Tuple1<A0>(
    val first: A0,
) {
    override fun toString(): String = "($first)"
}""")
    parts.append("""/**
 * A tuple with two elements. Typealias for [Pair].
 */
typealias Tuple2<A0, A1> = Pair<A0, A1>""")
    parts.append("""/**
 * A tuple with three elements. Typealias for [Triple].
 */
typealias Tuple3<A0, A1, A2> = Triple<A0, A1, A2>""")
    for n in range(4, max_n + 1):
        props = "\n".join("    val %s: A%d," % (ordinal(i + 1), i) for i in range(n))
        to_string = ", ".join("$" + o for o in ords(n))
        parts.append("/**\n * A tuple with %s elements.\n */\n"
                     "data class Tuple%d<%s>(\n%s\n) {\n"
                     '    override fun toString(): String = "(%s)"\n}'
                     % (cardinal(n), n, types(n), props, to_string))
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- TupleFactory.kt

def gen_factory(pkg, max_n):
    parts = ["""/**
 * Factory functions for creating Tuple instances.
 *
 * Provides overloaded [tupleOf] functions for 0 to %d elements.
 *
 * Usage:
 * ```kotlin
 * val empty = tupleOf()                    // Tuple0
 * val single = tupleOf(1)                  // Tuple1<Int>
 * val pair = tupleOf("a", 2)              // Tuple2<String, Int> (= Pair)
 * val quad = tupleOf(1, "a", true, 3.14)  // Tuple4<Int, String, Boolean, Double>
 * ```
 */
package %s""" % (max_n, pkg)]
    parts.append("/** Creates a [Tuple0] (empty tuple). */\nfun tupleOf(): Tuple0 = Tuple0")
    parts.append("/** Creates a [Tuple1] with a single element. */\n"
                 "fun <A0> tupleOf(first: A0): Tuple1<A0> = Tuple1(first)")
    parts.append("/** Creates a [Tuple2] (= [Pair]) with two elements. */\n"
                 "fun <A0, A1> tupleOf(first: A0, second: A1): Tuple2<A0, A1> = Tuple2(first, second)")
    parts.append("fun <A0, A1, A2> tupleOf(first: A0, second: A1, third: A2): "
                 "Tuple3<A0, A1, A2> = Tuple3(first, second, third)")
    for n in range(4, max_n + 1):
        params = ", ".join("%s: A%d" % (ordinal(i + 1), i) for i in range(n))
        args = ", ".join(ords(n))
        parts.append("fun <%s> tupleOf(%s): Tuple%d<%s> =\n    Tuple%d(%s)"
                     % (types(n), params, n, types(n), n, args))
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- TupleToList.kt

def gen_tolist(pkg, max_n):
    parts = ["""/**
 * Extension functions to convert Tuple instances to [List].
 *
 * Each [toList] function uses an upper-bound type parameter `Base` so that
 * the returned list has the most specific common supertype of all elements.
 *
 * Usage:
 * ```kotlin
 * val list: List<Int> = tupleOf(1, 2, 3).toList()         // [1, 2, 3]
 * val mixed: List<Any> = tupleOf(1, "a", true).toList()    // [1, "a", true]
 * ```
 */
package %s""" % pkg]
    parts.append("/** Converts a [Tuple0] to an empty list. */\n"
                 "fun Tuple0.toList(): List<Nothing> = emptyList()")
    parts.append("/** Converts a [Tuple1] to a single-element list. */\n"
                 "fun <Base, A0 : Base> Tuple1<A0>.toList(): List<Base> = listOf(first)")
    parts.append("/** Converts a [Tuple2] to a two-element list. */\n"
                 "fun <Base, A0 : Base, A1 : Base> Tuple2<A0, A1>.toList(): List<Base> = "
                 "listOf(first, second)")
    for n in range(3, max_n + 1):
        bounds = ", ".join("A%d : Base" % i for i in range(n))
        parts.append("fun <Base, %s> Tuple%d<%s>.toList(): List<Base> = listOf(%s)"
                     % (bounds, n, types(n), ", ".join(ords(n))))
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- AbstractTupleSerializer.kt

def gen_abstract_serializer(pkg, max_n):
    rows = []
    names = ['"%s"' % o for o in ords(max_n)]
    for i in range(0, len(names), 5):
        rows.append("    " + ", ".join(names[i:i + 5]) + ",")
    return """// NOTE: @file:OptIn must appear before the package declaration.
@file:OptIn(
    kotlinx.serialization.InternalSerializationApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class,
)

/**
 * Abstract base class for Tuple serializers.
 *
 * Provides common [KSerializer] logic (descriptor building, serialize, deserialize)
 * so that each concrete [TupleNSerializer][Tuple4Serializer] only needs to implement
 * [toValues] and [fromValues].
 *
 * @param T The Tuple type to serialize.
 * @param name The serial name used in the descriptor (e.g., "Tuple4").
 * @param serializers The element serializers in order.
 * @param elementNames The element names in order (e.g., "first", "second", ...).
 */
package %s

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

abstract class AbstractTupleSerializer<T>(
    name: String,
    private val serializers: Array<KSerializer<*>>,
    elementNames: Array<String>,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor =
        buildSerialDescriptor(name, StructureKind.LIST) {
            serializers.forEachIndexed { index, serializer ->
                element(elementNames[index], serializer.descriptor)
            }
        }

    /** Extracts the tuple's element values as an ordered array. */
    protected abstract fun toValues(value: T): Array<Any?>

    /** Constructs a tuple instance from an ordered array of decoded values. */
    protected abstract fun fromValues(values: Array<Any?>): T

    override fun serialize(encoder: Encoder, value: T) {
        val values = toValues(value)
        encoder.encodeStructure(descriptor) {
            serializers.forEachIndexed { index, serializer ->
                @Suppress("UNCHECKED_CAST")
                encodeSerializableElement(
                    descriptor, index, serializer as KSerializer<Any?>, values[index],
                )
            }
        }
    }

    override fun deserialize(decoder: Decoder): T =
        decoder.decodeStructure(descriptor) {
            val values = arrayOfNulls<Any?>(serializers.size)
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                if (index in serializers.indices) {
                    @Suppress("UNCHECKED_CAST")
                    values[index] = decodeSerializableElement(
                        descriptor, index, serializers[index] as KSerializer<Any?>,
                    )
                } else {
                    error("Unexpected index: $index")
                }
            }
            fromValues(values)
        }
}

/** Ordinal element names used by all Tuple serializers. */
internal val TUPLE_ELEMENT_NAMES = arrayOf(
%s
)
""" % (pkg, "\n".join(rows))


# ---------------------------------------------------------------- TupleSerializer.kt

def gen_serializer(pkg, max_n):
    provides = ("[Tuple4Serializer]" if max_n == 4
                else "[Tuple4Serializer]\u2013[Tuple%dSerializer]" % max_n)
    parts = ["""// NOTE: @file:OptIn must appear before the package declaration.
@file:OptIn(
    kotlinx.serialization.InternalSerializationApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class,
)

/**
 * [KSerializer] implementations for Tuple types to support kotlinx.serialization.
 *
 * Each Tuple is serialized as a JSON array (e.g., `[1, "hello", true]`).
 *
 * **Note**: [Tuple2] (= [Pair]) and [Tuple3] (= [Triple]) already have built-in serializers
 * in kotlinx.serialization, so no custom serializer is provided for them.
 *
 * Provides: [Tuple0Serializer], [Tuple1Serializer], %s.
 *
 * @see AbstractTupleSerializer Common serialization logic shared by all Tuple serializers.
 */
package %s

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure""" % (provides, pkg)]
    parts.append("""/**
 * Serializer for [Tuple0]. Serializes as an empty JSON array `[]`.
 */
object Tuple0Serializer : KSerializer<Tuple0> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("Tuple0", StructureKind.LIST)

    override fun serialize(encoder: Encoder, value: Tuple0) {
        encoder.encodeStructure(descriptor) {}
    }

    override fun deserialize(decoder: Decoder): Tuple0 =
        decoder.decodeStructure(descriptor) { Tuple0 }
}""")
    parts.append("// Tuple2 (= Pair) and Tuple3 (= Triple) have built-in serializers "
                 "in kotlinx.serialization.")
    parts.append("""/**
 * Serializer for [Tuple1]. Serializes as a single-element JSON array `[value]`.
 *
 * @param serializer0 Serializer for the first element.
 */
class Tuple1Serializer<A0>(
    serializer0: KSerializer<A0>,
) : AbstractTupleSerializer<Tuple1<A0>>(
    "Tuple1", arrayOf(serializer0), TUPLE_ELEMENT_NAMES.sliceArray(0..0),
) {
    override fun toValues(value: Tuple1<A0>): Array<Any?> = arrayOf(value.first)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple1<A0> =
        tupleOf(values[0] as A0)
}""")
    for n in range(4, max_n + 1):
        ctor_params = ["serializer%d: KSerializer<A%d>" % (i, i) for i in range(n)]
        ctor_lines = "\n".join(
            "    " + ", ".join(ctor_params[i:i + 2]) + ","
            for i in range(0, n, 2)
        )
        arr = ", ".join("serializer%d" % i for i in range(n))
        vals = ", ".join("value.%s" % o for o in ords(n))
        casts = ", ".join("values[%d] as A%d" % (i, i) for i in range(n))
        parts.append(
            "class Tuple%dSerializer<%s>(\n%s\n"
            ") : AbstractTupleSerializer<Tuple%d<%s>>(\n"
            '    "Tuple%d", arrayOf(%s),\n'
            "    TUPLE_ELEMENT_NAMES.sliceArray(0..%d),\n"
            ") {\n"
            "    override fun toValues(value: Tuple%d<%s>): Array<Any?> =\n"
            "        arrayOf(%s)\n"
            "\n"
            '    @Suppress("UNCHECKED_CAST")\n'
            "    override fun fromValues(values: Array<Any?>): Tuple%d<%s> =\n"
            "        tupleOf(%s)\n"
            "}"
            % (n, types(n), ctor_lines, n, types(n), n, arr, n - 1,
               n, types(n), vals, n, types(n), casts))
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- AwaitAll.kt

def gen_awaitall(pkg, max_n):
    parts = ["""/**
 * Type-safe [awaitAll] functions for multiple [Deferred] values.
 *
 * Unlike `kotlinx.coroutines.awaitAll` which returns `List<T>` (requiring a common type),
 * these overloads preserve each element's distinct type by returning a Tuple.
 *
 * Usage:
 * ```kotlin
 * val (name, age, active) = awaitAll(
 *     async { fetchName() },    // Deferred<String>
 *     async { fetchAge() },     // Deferred<Int>
 *     async { fetchActive() },  // Deferred<Boolean>
 * )
 * // name: String, age: Int, active: Boolean
 * ```
 *
 * Every overload has a Tuple receiver form, so a Tuple of [Deferred] can be awaited directly:
 * ```kotlin
 * val (name, age, active) = tupleOf(
 *     async { fetchName() },
 *     async { fetchAge() },
 *     async { fetchActive() },
 * ).awaitAll()
 * ```
 */
package %s

import kotlinx.coroutines.Deferred""" % pkg]
    for n in range(1, max_n + 1):
        params = "\n".join("    %s: Deferred<A%d>," % (ordinal(i + 1), i) for i in range(n))
        awaits = ", ".join("%s.await()" % o for o in ords(n))
        body = ("suspend fun <%s> awaitAll(\n%s\n): Tuple%d<%s> = tupleOf(%s)"
                % (types(n), params, n, types(n), awaits))
        if n == 1:
            body = ("/**\n * Awaits a single [Deferred] and wraps the result in a [Tuple1].\n */\n"
                    + body)
        parts.append(body)
        deferred_types = ", ".join("Deferred<A%d>" % i for i in range(n))
        parts.append("suspend fun <%s> Tuple%d<%s>.awaitAll(): Tuple%d<%s> = awaitAll(%s)"
                     % (types(n), n, deferred_types, n, types(n), ", ".join(ords(n))))
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- AllNotNullOrNull.kt

def gen_allnotnull(pkg, max_n):
    parts = ["""/**
 * Null-safety utilities for Tuple types.
 *
 * Provides [allNotNullOrNull] as both top-level functions and extension functions.
 * Returns a non-nullable Tuple if all elements are non-null, or `null` if any element is null.
 *
 * Uses the `val x = x ?: return null` early-return pattern for consistency and readability.
 *
 * Usage:
 * ```kotlin
 * val name: String? = ...
 * val age: Int? = ...
 * val result: Tuple2<String, Int>? = allNotNullOrNull(name, age)
 * // or
 * val result2: Tuple2<String, Int>? = tupleOf(name, age).allNotNullOrNull()
 * ```
 */
package %s""" % pkg]
    parts.append("""/**
 * Returns a [Tuple1] if [first] is non-null, or `null` otherwise.
 */
fun <A0 : Any> allNotNullOrNull(first: A0?): Tuple1<A0>? =
    tupleOf(first).allNotNullOrNull()

/**
 * Returns a non-nullable [Tuple1] if all elements are non-null, or `null` if any element is null.
 */
fun <A0 : Any> Tuple1<A0?>.allNotNullOrNull(): Tuple1<A0>? {
    val first = first ?: return null
    return tupleOf(first)
}""")
    for n in range(2, max_n + 1):
        if n == 2:
            comment = "// Tuple2 (= Pair)"
        elif n == 3:
            comment = "// Tuple3 (= Triple)"
        else:
            comment = "// Tuple%d" % n
        bounds = ", ".join("A%d : Any" % i for i in range(n))
        params = ", ".join("%s: A%d?" % (ordinal(i + 1), i) for i in range(n))
        nullable_types = ", ".join("A%d?" % i for i in range(n))
        args = ", ".join(ords(n))
        # NOTE: the golden example indents the first `val` with 8 spaces for n >= 6
        # (4 spaces for n <= 5). Kept as-is to stay byte-identical with example/.
        val_lines = []
        for i in range(n):
            o = ordinal(i + 1)
            indent = "        " if (i == 0 and n >= 6) else "    "
            val_lines.append("%sval %s = %s ?: return null" % (indent, o, o))
        parts.append(
            "%s\n\n"
            "fun <%s> allNotNullOrNull(%s): Tuple%d<%s>? =\n"
            "    tupleOf(%s).allNotNullOrNull()\n"
            "\n"
            "fun <%s> Tuple%d<%s>.allNotNullOrNull(): Tuple%d<%s>? {\n"
            "%s\n"
            "    return tupleOf(%s)\n"
            "}"
            % (comment, bounds, params, n, types(n), args,
               bounds, n, nullable_types, n, types(n),
               "\n".join(val_lines), args))
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- AwaitAllCatching.kt

def gen_awaitcatching(pkg, max_n):
    parts = ["""/**
 * Type-safe [awaitAllCatching] functions that never fail as a whole.
 *
 * Every block runs concurrently and its outcome is captured in a [Result], so a failure in one
 * block neither cancels the others nor propagates out of [awaitAllCatching]. The caller decides
 * what a partial failure means.
 *
 * The parameters are `suspend () -> T` blocks rather than `Deferred` values on purpose: a
 * `Deferred` created by the caller's `async` cancels its parent scope when it fails, which makes
 * per-element recovery impossible. Creating the coroutines inside this function is what keeps the
 * failures isolated.
 *
 * [CancellationException] is always rethrown, so cancelling the surrounding coroutine still works.
 *
 * Usage:
 * ```kotlin
 * val (name, age, active) = awaitAllCatching(
 *     { fetchName() },    // Result<String>
 *     { fetchAge() },     // Result<Int>
 *     { fetchActive() },  // Result<Boolean>
 * )
 * name.onFailure { log(it) }
 * val resolvedAge = age.getOrDefault(0)
 * ```
 *
 * The Tuple receiver form needs `suspend {}` literals, because a bare `{ ... }` passed to
 * `tupleOf` would be inferred as a non-suspending function type:
 * ```kotlin
 * val (name, age) = tupleOf(
 *     suspend { fetchName() },
 *     suspend { fetchAge() },
 * ).awaitAllCatching()
 * ```
 *
 * `allSuccessOrNull()` / `allSuccessOrFailure()` (TupleResult.kt) collapse the returned Tuple of
 * [Result] back into a single value.
 */
package %s

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope""" % pkg]
    parts.append("""/**
 * Runs [block] and captures its outcome in a [Result].
 *
 * Unlike [runCatching], [CancellationException] is rethrown instead of being captured as a
 * failure, so structured concurrency is preserved.
 */
private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }""")
    for n in range(1, max_n + 1):
        params = "\n".join("    %s: suspend () -> A%d," % (ordinal(i + 1), i) for i in range(n))
        result_types = ", ".join("Result<A%d>" % i for i in range(n))
        vals = "\n".join("    val %sDeferred = async { runCatchingCancellable(%s) }" % (o, o)
                         for o in ords(n))
        awaits = ", ".join("%sDeferred.await()" % o for o in ords(n))
        body = ("suspend fun <%s> awaitAllCatching(\n%s\n): Tuple%d<%s> = coroutineScope {\n"
                "%s\n    tupleOf(%s)\n}"
                % (types(n), params, n, result_types, vals, awaits))
        if n == 1:
            body = ("/**\n * Runs a single block and wraps its outcome in a [Tuple1] of [Result].\n */\n"
                    + body)
        parts.append(body)
        lambda_types = ", ".join("suspend () -> A%d" % i for i in range(n))
        parts.append("suspend fun <%s> Tuple%d<%s>.awaitAllCatching(): Tuple%d<%s> =\n"
                     "    awaitAllCatching(%s)"
                     % (types(n), n, lambda_types, n, result_types, ", ".join(ords(n))))
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- TupleResult.kt

def gen_result(pkg, max_n):
    parts = ["""/**
 * Utilities for collapsing a Tuple of [Result] values into a single value.
 *
 * Designed to pair with `awaitAllCatching` (AwaitAllCatching.kt), which returns
 * `TupleN<Result<A0>, ..>`, but works with any Tuple of [Result] -- for example one built from
 * [runCatching].
 *
 * Unlike `Result.getOrNull()`, a successful `null` value is preserved: only [Result.isFailure]
 * decides whether the whole Tuple collapses.
 *
 * Usage:
 * ```kotlin
 * val results: Tuple2<Result<String>, Result<Int>> = tupleOf(
 *     runCatching { parseName() },
 *     runCatching { parseAge() },
 * )
 *
 * val values: Tuple2<String, Int>? = results.allSuccessOrNull()
 * val single: Result<Tuple2<String, Int>> = results.allSuccessOrFailure()
 * ```
 */
package %s""" % pkg]
    for n in range(1, max_n + 1):
        result_types = ", ".join("Result<A%d>" % i for i in range(n))
        null_args = "\n".join("        %s.getOrElse { return null }," % o for o in ords(n))
        fail_args = "\n".join("            %s.getOrElse { return Result.failure(it) }," % o
                              for o in ords(n))
        or_null = ("fun <%s> Tuple%d<%s>.allSuccessOrNull(): Tuple%d<%s>? {\n"
                   "    return tupleOf(\n%s\n    )\n}"
                   % (types(n), n, result_types, n, types(n), null_args))
        or_failure = ("fun <%s> Tuple%d<%s>.allSuccessOrFailure(): Result<Tuple%d<%s>> {\n"
                      "    return Result.success(\n        tupleOf(\n%s\n        ),\n    )\n}"
                      % (types(n), n, result_types, n, types(n), fail_args))
        if n == 1:
            or_null = ("/**\n * Returns the success values as a [Tuple1], "
                       "or `null` if the element is a failure.\n */\n" + or_null)
            or_failure = ("/**\n * Returns the success values as a [Tuple1], "
                          "or the failure itself.\n */\n" + or_failure)
        else:
            if n == 2:
                header = "// Tuple2 (= Pair)"
            elif n == 3:
                header = "// Tuple3 (= Triple)"
            else:
                header = "// Tuple%d" % n
            or_null = header + "\n\n" + or_null
        parts.append(or_null)
        parts.append(or_failure)
    return "\n\n".join(parts) + "\n"


# ---------------------------------------------------------------- parts / files

PART_FILES = {
    "tuple": [("Tuple.kt", gen_tuple)],
    "factory": [("TupleFactory.kt", gen_factory)],
    "tolist": [("TupleToList.kt", gen_tolist)],
    "serializer": [("AbstractTupleSerializer.kt", gen_abstract_serializer),
                   ("TupleSerializer.kt", gen_serializer)],
    "awaitall": [("AwaitAll.kt", gen_awaitall)],
    "awaitcatching": [("AwaitAllCatching.kt", gen_awaitcatching)],
    "allnotnull": [("AllNotNullOrNull.kt", gen_allnotnull)],
    "result": [("TupleResult.kt", gen_result)],
}
ALL_PARTS = ["tuple", "factory", "tolist", "serializer", "awaitall", "awaitcatching",
             "allnotnull", "result"]
REQUIRED_PARTS = ["tuple", "factory"]


def render(pkg, max_n, parts):
    """Returns {filename: content} for the selected parts, in canonical order."""
    out = {}
    for part in ALL_PARTS:
        if part in parts:
            for filename, fn in PART_FILES[part]:
                out[filename] = fn(pkg, max_n)
    return out
