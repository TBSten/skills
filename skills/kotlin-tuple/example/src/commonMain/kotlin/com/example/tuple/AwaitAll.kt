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
 */
package com.example.tuple

import kotlinx.coroutines.Deferred

/**
 * Awaits a single [Deferred] and wraps the result in a [Tuple1].
 */
suspend fun <A0> awaitAll(
    first: Deferred<A0>,
): Tuple1<A0> = tupleOf(first.await())

suspend fun <A0, A1> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
): Tuple2<A0, A1> = tupleOf(first.await(), second.await())

suspend fun <A0, A1, A2> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
): Tuple3<A0, A1, A2> = tupleOf(first.await(), second.await(), third.await())

suspend fun <A0, A1, A2, A3> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
): Tuple4<A0, A1, A2, A3> = tupleOf(first.await(), second.await(), third.await(), fourth.await())

suspend fun <A0, A1, A2, A3, A4> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
): Tuple5<A0, A1, A2, A3, A4> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await())

suspend fun <A0, A1, A2, A3, A4, A5> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
): Tuple6<A0, A1, A2, A3, A4, A5> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await())

suspend fun <A0, A1, A2, A3, A4, A5, A6> awaitAll(
    first: Deferred<A0>,
    second: Deferred<A1>,
    third: Deferred<A2>,
    fourth: Deferred<A3>,
    fifth: Deferred<A4>,
    sixth: Deferred<A5>,
    seventh: Deferred<A6>,
): Tuple7<A0, A1, A2, A3, A4, A5, A6> = tupleOf(first.await(), second.await(), third.await(), fourth.await(), fifth.await(), sixth.await(), seventh.await())

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
