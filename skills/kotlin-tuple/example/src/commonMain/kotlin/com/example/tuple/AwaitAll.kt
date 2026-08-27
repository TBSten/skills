/**
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
package com.example.tuple

import kotlinx.coroutines.Deferred

/**
 * Awaits a single [Deferred] and wraps the result in a [Tuple1].
 */
suspend fun <A0> awaitAll(
    first: Deferred<A0>,
): Tuple1<A0> = tupleOf(first.await())

suspend fun <A0> Tuple1<Deferred<A0>>.awaitAll(): Tuple1<A0> = awaitAll(first)

suspend fun <A0, A1> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
): Tuple2<A0, A1> = tupleOf(first.await(), second.await())

suspend fun <A0, A1> Tuple2<Deferred<A0>, Deferred<A1>>.awaitAll(): Tuple2<A0, A1> = awaitAll(first, second)

suspend fun <A0, A1, A2> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
): Tuple3<A0, A1, A2> = tupleOf(first.await(), second.await(), third.await())

suspend fun <A0, A1, A2> Tuple3<Deferred<A0>, Deferred<A1>, Deferred<A2>>.awaitAll(): Tuple3<A0, A1, A2> = awaitAll(first, second, third)

suspend fun <A0, A1, A2, A3> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
): Tuple4<A0, A1, A2, A3> = tupleOf(first.await(), second.await(), third.await(), fourth.await())

suspend fun <A0, A1, A2, A3> Tuple4<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>>.awaitAll(): Tuple4<A0, A1, A2, A3> = awaitAll(first, second, third, fourth)

suspend fun <A0, A1, A2, A3, A4> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
): Tuple5<A0, A1, A2, A3, A4> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await())

suspend fun <A0, A1, A2, A3, A4> Tuple5<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>>.awaitAll(): Tuple5<A0, A1, A2, A3, A4> = awaitAll(first, second, third, fourth, fifth)

suspend fun <A0, A1, A2, A3, A4, A5> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
): Tuple6<A0, A1, A2, A3, A4, A5> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await())

suspend fun <A0, A1, A2, A3, A4, A5> Tuple6<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>>.awaitAll(): Tuple6<A0, A1, A2, A3, A4, A5> = awaitAll(first, second, third, fourth, fifth, sixth)

suspend fun <A0, A1, A2, A3, A4, A5, A6> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
): Tuple7<A0, A1, A2, A3, A4, A5, A6> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6> Tuple7<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>>.awaitAll(): Tuple7<A0, A1, A2, A3, A4, A5, A6> = awaitAll(first, second, third, fourth, fifth, sixth, seventh)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
): Tuple8<A0, A1, A2, A3, A4, A5, A6, A7> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7> Tuple8<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>>.awaitAll(): Tuple8<A0, A1, A2, A3, A4, A5, A6, A7> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
): Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8> Tuple9<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>>.awaitAll(): Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
): Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> Tuple10<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>>.awaitAll(): Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
): Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> Tuple11<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>>.awaitAll(): Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
): Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> Tuple12<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>>.awaitAll(): Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
): Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> Tuple13<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>>.awaitAll(): Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
    fourteenth: Deferred<A13>,
): Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await(), fourteenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> Tuple14<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>, Deferred<A13>>.awaitAll(): Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
    fourteenth: Deferred<A13>,
    fifteenth: Deferred<A14>,
): Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await(), fourteenth.await(), fifteenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> Tuple15<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>, Deferred<A13>, Deferred<A14>>.awaitAll(): Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
    fourteenth: Deferred<A13>,
    fifteenth: Deferred<A14>,
    sixteenth: Deferred<A15>,
): Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await(), fourteenth.await(), fifteenth.await(), sixteenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> Tuple16<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>, Deferred<A13>, Deferred<A14>, Deferred<A15>>.awaitAll(): Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
    fourteenth: Deferred<A13>,
    fifteenth: Deferred<A14>,
    sixteenth: Deferred<A15>,
    seventeenth: Deferred<A16>,
): Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await(), fourteenth.await(), fifteenth.await(), sixteenth.await(), seventeenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> Tuple17<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>, Deferred<A13>, Deferred<A14>, Deferred<A15>, Deferred<A16>>.awaitAll(): Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
    fourteenth: Deferred<A13>,
    fifteenth: Deferred<A14>,
    sixteenth: Deferred<A15>,
    seventeenth: Deferred<A16>,
    eighteenth: Deferred<A17>,
): Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await(), fourteenth.await(), fifteenth.await(), sixteenth.await(), seventeenth.await(), eighteenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> Tuple18<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>, Deferred<A13>, Deferred<A14>, Deferred<A15>, Deferred<A16>, Deferred<A17>>.awaitAll(): Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
    fourteenth: Deferred<A13>,
    fifteenth: Deferred<A14>,
    sixteenth: Deferred<A15>,
    seventeenth: Deferred<A16>,
    eighteenth: Deferred<A17>,
    nineteenth: Deferred<A18>,
): Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await(), fourteenth.await(), fifteenth.await(), sixteenth.await(), seventeenth.await(), eighteenth.await(), nineteenth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> Tuple19<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>, Deferred<A13>, Deferred<A14>, Deferred<A15>, Deferred<A16>, Deferred<A17>, Deferred<A18>>.awaitAll(): Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth)

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
    eighth: Deferred<A7>,
    ninth: Deferred<A8>,
    tenth: Deferred<A9>,
    eleventh: Deferred<A10>,
    twelfth: Deferred<A11>,
    thirteenth: Deferred<A12>,
    fourteenth: Deferred<A13>,
    fifteenth: Deferred<A14>,
    sixteenth: Deferred<A15>,
    seventeenth: Deferred<A16>,
    eighteenth: Deferred<A17>,
    nineteenth: Deferred<A18>,
    twentieth: Deferred<A19>,
): Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await(), eighth.await(), ninth.await(), tenth.await(), eleventh.await(), twelfth.await(), thirteenth.await(), fourteenth.await(), fifteenth.await(), sixteenth.await(), seventeenth.await(), eighteenth.await(), nineteenth.await(), twentieth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> Tuple20<Deferred<A0>, Deferred<A1>, Deferred<A2>, Deferred<A3>, Deferred<A4>, Deferred<A5>, Deferred<A6>, Deferred<A7>, Deferred<A8>, Deferred<A9>, Deferred<A10>, Deferred<A11>, Deferred<A12>, Deferred<A13>, Deferred<A14>, Deferred<A15>, Deferred<A16>, Deferred<A17>, Deferred<A18>, Deferred<A19>>.awaitAll(): Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> = awaitAll(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth, twentieth)
