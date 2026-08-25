/**
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
package com.example.tuple

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
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
    }

/**
 * Runs a single block and wraps its outcome in a [Tuple1] of [Result].
 */
suspend fun <A0> awaitAllCatching(
    first: suspend () -> A0,
): Tuple1<Result<A0>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    tupleOf(firstDeferred.await())
}

suspend fun <A0> Tuple1<suspend () -> A0>.awaitAllCatching(): Tuple1<Result<A0>> =
    awaitAllCatching(first)

suspend fun <A0, A1> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
): Tuple2<Result<A0>, Result<A1>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    tupleOf(firstDeferred.await(), secondDeferred.await())
}

suspend fun <A0, A1> Tuple2<suspend () -> A0, suspend () -> A1>.awaitAllCatching(): Tuple2<Result<A0>, Result<A1>> =
    awaitAllCatching(first, second)

suspend fun <A0, A1, A2> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
): Tuple3<Result<A0>, Result<A1>, Result<A2>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await())
}

suspend fun <A0, A1, A2> Tuple3<suspend () -> A0, suspend () -> A1, suspend () -> A2>.awaitAllCatching(): Tuple3<Result<A0>, Result<A1>, Result<A2>> =
    awaitAllCatching(first, second, third)

suspend fun <A0, A1, A2, A3> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
): Tuple4<Result<A0>, Result<A1>, Result<A2>, Result<A3>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await())
}

suspend fun <A0, A1, A2, A3> Tuple4<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3>.awaitAllCatching(): Tuple4<Result<A0>, Result<A1>, Result<A2>, Result<A3>> =
    awaitAllCatching(first, second, third, fourth)

suspend fun <A0, A1, A2, A3, A4> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
): Tuple5<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4> Tuple5<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4>.awaitAllCatching(): Tuple5<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>> =
    awaitAllCatching(first, second, third, fourth, fifth)

suspend fun <A0, A1, A2, A3, A4, A5> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
): Tuple6<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5> Tuple6<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5>.awaitAllCatching(): Tuple6<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth)

suspend fun <A0, A1, A2, A3, A4, A5, A6> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
): Tuple7<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6> Tuple7<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6>.awaitAllCatching(): Tuple7<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
): Tuple8<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7> Tuple8<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7>.awaitAllCatching(): Tuple8<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
): Tuple9<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8> Tuple9<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8>.awaitAllCatching(): Tuple9<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
): Tuple10<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> Tuple10<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9>.awaitAllCatching(): Tuple10<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
): Tuple11<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> Tuple11<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10>.awaitAllCatching(): Tuple11<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
): Tuple12<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> Tuple12<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11>.awaitAllCatching(): Tuple12<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
): Tuple13<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> Tuple13<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12>.awaitAllCatching(): Tuple13<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
    fourteenth: suspend () -> A13,
): Tuple14<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    val fourteenthDeferred = async { runCatchingCancellable(fourteenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await(), fourteenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> Tuple14<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12, suspend () -> A13>.awaitAllCatching(): Tuple14<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
    fourteenth: suspend () -> A13,
    fifteenth: suspend () -> A14,
): Tuple15<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    val fourteenthDeferred = async { runCatchingCancellable(fourteenth) }
    val fifteenthDeferred = async { runCatchingCancellable(fifteenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await(), fourteenthDeferred.await(), fifteenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> Tuple15<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12, suspend () -> A13, suspend () -> A14>.awaitAllCatching(): Tuple15<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
    fourteenth: suspend () -> A13,
    fifteenth: suspend () -> A14,
    sixteenth: suspend () -> A15,
): Tuple16<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    val fourteenthDeferred = async { runCatchingCancellable(fourteenth) }
    val fifteenthDeferred = async { runCatchingCancellable(fifteenth) }
    val sixteenthDeferred = async { runCatchingCancellable(sixteenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await(), fourteenthDeferred.await(), fifteenthDeferred.await(), sixteenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> Tuple16<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12, suspend () -> A13, suspend () -> A14, suspend () -> A15>.awaitAllCatching(): Tuple16<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
    fourteenth: suspend () -> A13,
    fifteenth: suspend () -> A14,
    sixteenth: suspend () -> A15,
    seventeenth: suspend () -> A16,
): Tuple17<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    val fourteenthDeferred = async { runCatchingCancellable(fourteenth) }
    val fifteenthDeferred = async { runCatchingCancellable(fifteenth) }
    val sixteenthDeferred = async { runCatchingCancellable(sixteenth) }
    val seventeenthDeferred = async { runCatchingCancellable(seventeenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await(), fourteenthDeferred.await(), fifteenthDeferred.await(), sixteenthDeferred.await(), seventeenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> Tuple17<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12, suspend () -> A13, suspend () -> A14, suspend () -> A15, suspend () -> A16>.awaitAllCatching(): Tuple17<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
    fourteenth: suspend () -> A13,
    fifteenth: suspend () -> A14,
    sixteenth: suspend () -> A15,
    seventeenth: suspend () -> A16,
    eighteenth: suspend () -> A17,
): Tuple18<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    val fourteenthDeferred = async { runCatchingCancellable(fourteenth) }
    val fifteenthDeferred = async { runCatchingCancellable(fifteenth) }
    val sixteenthDeferred = async { runCatchingCancellable(sixteenth) }
    val seventeenthDeferred = async { runCatchingCancellable(seventeenth) }
    val eighteenthDeferred = async { runCatchingCancellable(eighteenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await(), fourteenthDeferred.await(), fifteenthDeferred.await(), sixteenthDeferred.await(), seventeenthDeferred.await(), eighteenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> Tuple18<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12, suspend () -> A13, suspend () -> A14, suspend () -> A15, suspend () -> A16, suspend () -> A17>.awaitAllCatching(): Tuple18<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
    fourteenth: suspend () -> A13,
    fifteenth: suspend () -> A14,
    sixteenth: suspend () -> A15,
    seventeenth: suspend () -> A16,
    eighteenth: suspend () -> A17,
    nineteenth: suspend () -> A18,
): Tuple19<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    val fourteenthDeferred = async { runCatchingCancellable(fourteenth) }
    val fifteenthDeferred = async { runCatchingCancellable(fifteenth) }
    val sixteenthDeferred = async { runCatchingCancellable(sixteenth) }
    val seventeenthDeferred = async { runCatchingCancellable(seventeenth) }
    val eighteenthDeferred = async { runCatchingCancellable(eighteenth) }
    val nineteenthDeferred = async { runCatchingCancellable(nineteenth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await(), fourteenthDeferred.await(), fifteenthDeferred.await(), sixteenthDeferred.await(), seventeenthDeferred.await(), eighteenthDeferred.await(), nineteenthDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> Tuple19<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12, suspend () -> A13, suspend () -> A14, suspend () -> A15, suspend () -> A16, suspend () -> A17, suspend () -> A18>.awaitAllCatching(): Tuple19<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> awaitAllCatching(
    first: suspend () -> A0,
    second: suspend () -> A1,
    third: suspend () -> A2,
    fourth: suspend () -> A3,
    fifth: suspend () -> A4,
    sixth: suspend () -> A5,
    seventh: suspend () -> A6,
    eighth: suspend () -> A7,
    ninth: suspend () -> A8,
    tenth: suspend () -> A9,
    eleventh: suspend () -> A10,
    twelfth: suspend () -> A11,
    thirteenth: suspend () -> A12,
    fourteenth: suspend () -> A13,
    fifteenth: suspend () -> A14,
    sixteenth: suspend () -> A15,
    seventeenth: suspend () -> A16,
    eighteenth: suspend () -> A17,
    nineteenth: suspend () -> A18,
    twentieth: suspend () -> A19,
): Tuple20<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>, Result<A19>> = coroutineScope {
    val firstDeferred = async { runCatchingCancellable(first) }
    val secondDeferred = async { runCatchingCancellable(second) }
    val thirdDeferred = async { runCatchingCancellable(third) }
    val fourthDeferred = async { runCatchingCancellable(fourth) }
    val fifthDeferred = async { runCatchingCancellable(fifth) }
    val sixthDeferred = async { runCatchingCancellable(sixth) }
    val seventhDeferred = async { runCatchingCancellable(seventh) }
    val eighthDeferred = async { runCatchingCancellable(eighth) }
    val ninthDeferred = async { runCatchingCancellable(ninth) }
    val tenthDeferred = async { runCatchingCancellable(tenth) }
    val eleventhDeferred = async { runCatchingCancellable(eleventh) }
    val twelfthDeferred = async { runCatchingCancellable(twelfth) }
    val thirteenthDeferred = async { runCatchingCancellable(thirteenth) }
    val fourteenthDeferred = async { runCatchingCancellable(fourteenth) }
    val fifteenthDeferred = async { runCatchingCancellable(fifteenth) }
    val sixteenthDeferred = async { runCatchingCancellable(sixteenth) }
    val seventeenthDeferred = async { runCatchingCancellable(seventeenth) }
    val eighteenthDeferred = async { runCatchingCancellable(eighteenth) }
    val nineteenthDeferred = async { runCatchingCancellable(nineteenth) }
    val twentiethDeferred = async { runCatchingCancellable(twentieth) }
    tupleOf(firstDeferred.await(), secondDeferred.await(), thirdDeferred.await(), fourthDeferred.await(), fifthDeferred.await(), sixthDeferred.await(), seventhDeferred.await(), eighthDeferred.await(), ninthDeferred.await(), tenthDeferred.await(), eleventhDeferred.await(), twelfthDeferred.await(), thirteenthDeferred.await(), fourteenthDeferred.await(), fifteenthDeferred.await(), sixteenthDeferred.await(), seventeenthDeferred.await(), eighteenthDeferred.await(), nineteenthDeferred.await(), twentiethDeferred.await())
}

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> Tuple20<suspend () -> A0, suspend () -> A1, suspend () -> A2, suspend () -> A3, suspend () -> A4, suspend () -> A5, suspend () -> A6, suspend () -> A7, suspend () -> A8, suspend () -> A9, suspend () -> A10, suspend () -> A11, suspend () -> A12, suspend () -> A13, suspend () -> A14, suspend () -> A15, suspend () -> A16, suspend () -> A17, suspend () -> A18, suspend () -> A19>.awaitAllCatching(): Tuple20<Result<A0>, Result<A1>, Result<A2>, Result<A3>, Result<A4>, Result<A5>, Result<A6>, Result<A7>, Result<A8>, Result<A9>, Result<A10>, Result<A11>, Result<A12>, Result<A13>, Result<A14>, Result<A15>, Result<A16>, Result<A17>, Result<A18>, Result<A19>> =
    awaitAllCatching(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth, twentieth)
