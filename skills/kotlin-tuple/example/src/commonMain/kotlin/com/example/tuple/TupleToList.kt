/**
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
package com.example.tuple

/** Converts a [Tuple0] to an empty list. */
fun Tuple0.toList(): List<Nothing> = emptyList()

/** Converts a [Tuple1] to a single-element list. */
fun <Base, A0 : Base> Tuple1<A0>.toList(): List<Base> = listOf(first)

/** Converts a [Tuple2] to a two-element list. */
fun <Base, A0 : Base, A1 : Base> Tuple2<A0, A1>.toList(): List<Base> = listOf(first, second)

fun <Base, A0 : Base, A1 : Base, A2 : Base> Tuple3<A0, A1, A2>.toList(): List<Base> = listOf(first, second, third)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base> Tuple4<A0, A1, A2, A3>.toList(): List<Base> = listOf(first, second, third, fourth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base> Tuple5<A0, A1, A2, A3, A4>.toList(): List<Base> = listOf(first, second, third, fourth, fifth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base> Tuple6<A0, A1, A2, A3, A4, A5>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base> Tuple7<A0, A1, A2, A3, A4, A5, A6>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base> Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base> Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base> Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base> Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base> Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base> Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base, A13 : Base> Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base, A13 : Base, A14 : Base> Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base, A13 : Base, A14 : Base, A15 : Base> Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base, A13 : Base, A14 : Base, A15 : Base, A16 : Base> Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base, A13 : Base, A14 : Base, A15 : Base, A16 : Base, A17 : Base> Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base, A13 : Base, A14 : Base, A15 : Base, A16 : Base, A17 : Base, A18 : Base> Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth)

fun <Base, A0 : Base, A1 : Base, A2 : Base, A3 : Base, A4 : Base, A5 : Base, A6 : Base, A7 : Base, A8 : Base, A9 : Base, A10 : Base, A11 : Base, A12 : Base, A13 : Base, A14 : Base, A15 : Base, A16 : Base, A17 : Base, A18 : Base, A19 : Base> Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>.toList(): List<Base> = listOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth, twentieth)
