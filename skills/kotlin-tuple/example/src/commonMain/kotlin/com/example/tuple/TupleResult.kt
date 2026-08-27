/**
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
package com.example.tuple

/**
 * Returns the success values as a [Tuple1], or `null` if the element is a failure.
 */
fun <A0> Tuple1<Result<A0>>.allSuccessOrNull(): Tuple1<A0>? {
    return tupleOf(
        first.getOrElse { return null },
    )
}

/**
 * Returns the success values as a [Tuple1], or the failure itself.
 */
fun <A0> Tuple1<Result<A0>>.allSuccessOrFailure(): Result<Tuple1<A0>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple2 (= Pair)

fun <A0, A1> Tuple2<Result<A0>, Result<A1>>.allSuccessOrNull(): Tuple2<A0, A1>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
    )
}

fun <A0, A1> Tuple2<Result<A0>, Result<A1>>.allSuccessOrFailure(): Result<Tuple2<A0, A1>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple3 (= Triple)

fun <A0, A1, A2> Tuple3<Result<A0>, Result<A1>, Result<A2>>.allSuccessOrNull(): Tuple3<A0, A1, A2>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
    )
}

fun <A0, A1, A2> Tuple3<Result<A0>, Result<A1>, Result<A2>>.allSuccessOrFailure(): Result<Tuple3<A0, A1, A2>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple4

fun <A0, A1, A2, A3> Tuple4<Result<A0>, Result<A1>, Result<A2>, Result<A3>>.allSuccessOrNull(): Tuple4<A0, A1, A2, A3>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3> Tuple4<Result<A0>, Result<A1>, Result<A2>, Result<A3>>.allSuccessOrFailure(): Result<Tuple4<A0, A1, A2, A3>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple5

fun <A0, A1, A2, A3, A4> Tuple5<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>>.allSuccessOrNull(): Tuple5<A0, A1, A2, A3, A4>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4> Tuple5<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>>.allSuccessOrFailure(): Result<Tuple5<A0, A1, A2, A3, A4>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple6

fun <A0, A1, A2, A3, A4, A5> Tuple6<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>>.allSuccessOrNull(): Tuple6<A0, A1, A2, A3, A4, A5>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5> Tuple6<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>>.allSuccessOrFailure(): Result<Tuple6<A0, A1, A2, A3, A4, A5>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple7

fun <A0, A1, A2, A3, A4, A5, A6> Tuple7<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>>.allSuccessOrNull(): Tuple7<A0, A1, A2, A3, A4, A5, A6>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6> Tuple7<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>>.allSuccessOrFailure(): Result<Tuple7<A0, A1, A2, A3, A4, A5, A6>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple8

fun <A0, A1, A2, A3, A4, A5, A6, A7> Tuple8<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>>.allSuccessOrNull(): Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7> Tuple8<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>>.allSuccessOrFailure(): Result<Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple9

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8> Tuple9<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>>.allSuccessOrNull(): Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8> Tuple9<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>>.allSuccessOrFailure(): Result<Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple10

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> Tuple10<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>>.allSuccessOrNull(): Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> Tuple10<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>>.allSuccessOrFailure(): Result<Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple11

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> Tuple11<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>>.allSuccessOrNull(): Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> Tuple11<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>>.allSuccessOrFailure(): Result<Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple12

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> Tuple12<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>>.allSuccessOrNull(): Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> Tuple12<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>>.allSuccessOrFailure(): Result<Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple13

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> Tuple13<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>>.allSuccessOrNull(): Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> Tuple13<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>>.allSuccessOrFailure(): Result<Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple14

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> Tuple14<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>>.allSuccessOrNull(): Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
        fourteenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> Tuple14<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>>.allSuccessOrFailure(): Result<Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
            fourteenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple15

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> Tuple15<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>>.allSuccessOrNull(): Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
        fourteenth.getOrElse { return null },
        fifteenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> Tuple15<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>>.allSuccessOrFailure(): Result<Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
            fourteenth.getOrElse { return Result.failure(it) },
            fifteenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple16

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> Tuple16<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>>.allSuccessOrNull(): Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
        fourteenth.getOrElse { return null },
        fifteenth.getOrElse { return null },
        sixteenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> Tuple16<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>>.allSuccessOrFailure(): Result<Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
            fourteenth.getOrElse { return Result.failure(it) },
            fifteenth.getOrElse { return Result.failure(it) },
            sixteenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple17

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> Tuple17<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>>.allSuccessOrNull(): Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
        fourteenth.getOrElse { return null },
        fifteenth.getOrElse { return null },
        sixteenth.getOrElse { return null },
        seventeenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> Tuple17<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>>.allSuccessOrFailure(): Result<Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
            fourteenth.getOrElse { return Result.failure(it) },
            fifteenth.getOrElse { return Result.failure(it) },
            sixteenth.getOrElse { return Result.failure(it) },
            seventeenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple18

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> Tuple18<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>>.allSuccessOrNull(): Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
        fourteenth.getOrElse { return null },
        fifteenth.getOrElse { return null },
        sixteenth.getOrElse { return null },
        seventeenth.getOrElse { return null },
        eighteenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> Tuple18<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>>.allSuccessOrFailure(): Result<Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
            fourteenth.getOrElse { return Result.failure(it) },
            fifteenth.getOrElse { return Result.failure(it) },
            sixteenth.getOrElse { return Result.failure(it) },
            seventeenth.getOrElse { return Result.failure(it) },
            eighteenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple19

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> Tuple19<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>>.allSuccessOrNull(): Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
        fourteenth.getOrElse { return null },
        fifteenth.getOrElse { return null },
        sixteenth.getOrElse { return null },
        seventeenth.getOrElse { return null },
        eighteenth.getOrElse { return null },
        nineteenth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> Tuple19<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>>.allSuccessOrFailure(): Result<Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
            fourteenth.getOrElse { return Result.failure(it) },
            fifteenth.getOrElse { return Result.failure(it) },
            sixteenth.getOrElse { return Result.failure(it) },
            seventeenth.getOrElse { return Result.failure(it) },
            eighteenth.getOrElse { return Result.failure(it) },
            nineteenth.getOrElse { return Result.failure(it) },
        ),
    )
}

// Tuple20

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> Tuple20<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>, Result<A19>>.allSuccessOrNull(): Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>? {
    return tupleOf(
        first.getOrElse { return null },
        second.getOrElse { return null },
        third.getOrElse { return null },
        fourth.getOrElse { return null },
        fifth.getOrElse { return null },
        sixth.getOrElse { return null },
        seventh.getOrElse { return null },
        eighth.getOrElse { return null },
        ninth.getOrElse { return null },
        tenth.getOrElse { return null },
        eleventh.getOrElse { return null },
        twelfth.getOrElse { return null },
        thirteenth.getOrElse { return null },
        fourteenth.getOrElse { return null },
        fifteenth.getOrElse { return null },
        sixteenth.getOrElse { return null },
        seventeenth.getOrElse { return null },
        eighteenth.getOrElse { return null },
        nineteenth.getOrElse { return null },
        twentieth.getOrElse { return null },
    )
}

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> Tuple20<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>, Result<A19>>.allSuccessOrFailure(): Result<Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>> {
    return Result.success(
        tupleOf(
            first.getOrElse { return Result.failure(it) },
            second.getOrElse { return Result.failure(it) },
            third.getOrElse { return Result.failure(it) },
            fourth.getOrElse { return Result.failure(it) },
            fifth.getOrElse { return Result.failure(it) },
            sixth.getOrElse { return Result.failure(it) },
            seventh.getOrElse { return Result.failure(it) },
            eighth.getOrElse { return Result.failure(it) },
            ninth.getOrElse { return Result.failure(it) },
            tenth.getOrElse { return Result.failure(it) },
            eleventh.getOrElse { return Result.failure(it) },
            twelfth.getOrElse { return Result.failure(it) },
            thirteenth.getOrElse { return Result.failure(it) },
            fourteenth.getOrElse { return Result.failure(it) },
            fifteenth.getOrElse { return Result.failure(it) },
            sixteenth.getOrElse { return Result.failure(it) },
            seventeenth.getOrElse { return Result.failure(it) },
            eighteenth.getOrElse { return Result.failure(it) },
            nineteenth.getOrElse { return Result.failure(it) },
            twentieth.getOrElse { return Result.failure(it) },
        ),
    )
}
