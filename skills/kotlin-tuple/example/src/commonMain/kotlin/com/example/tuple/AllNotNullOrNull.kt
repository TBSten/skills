/**
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
package com.example.tuple

/**
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
}

// Tuple2 (= Pair)

fun <A0 : Any, A1 : Any> allNotNullOrNull(first: A0?, second: A1?): Tuple2<A0, A1>? =
    tupleOf(first, second).allNotNullOrNull()

fun <A0 : Any, A1 : Any> Tuple2<A0?, A1?>.allNotNullOrNull(): Tuple2<A0, A1>? {
    val first = first ?: return null
    val second = second ?: return null
    return tupleOf(first, second)
}

// Tuple3 (= Triple)

fun <A0 : Any, A1 : Any, A2 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?): Tuple3<A0, A1, A2>? =
    tupleOf(first, second, third).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any> Tuple3<A0?, A1?, A2?>.allNotNullOrNull(): Tuple3<A0, A1, A2>? {
    val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    return tupleOf(first, second, third)
}

// Tuple4

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?): Tuple4<A0, A1, A2, A3>? =
    tupleOf(first, second, third, fourth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any> Tuple4<A0?, A1?, A2?, A3?>.allNotNullOrNull(): Tuple4<A0, A1, A2, A3>? {
    val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    return tupleOf(first, second, third, fourth)
}

// Tuple5

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?): Tuple5<A0, A1, A2, A3, A4>? =
    tupleOf(first, second, third, fourth, fifth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any> Tuple5<A0?, A1?, A2?, A3?, A4?>.allNotNullOrNull(): Tuple5<A0, A1, A2, A3, A4>? {
    val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    return tupleOf(first, second, third, fourth, fifth)
}

// Tuple6

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?): Tuple6<A0, A1, A2, A3, A4, A5>? =
    tupleOf(first, second, third, fourth, fifth, sixth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any> Tuple6<A0?, A1?, A2?, A3?, A4?, A5?>.allNotNullOrNull(): Tuple6<A0, A1, A2, A3, A4, A5>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth)
}

// Tuple7

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?): Tuple7<A0, A1, A2, A3, A4, A5, A6>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any> Tuple7<A0?, A1?, A2?, A3?, A4?, A5?, A6?>.allNotNullOrNull(): Tuple7<A0, A1, A2, A3, A4, A5, A6>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh)
}

// Tuple8

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?): Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any> Tuple8<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?>.allNotNullOrNull(): Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth)
}

// Tuple9

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?): Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any> Tuple9<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?>.allNotNullOrNull(): Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth)
}

// Tuple10

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?): Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any> Tuple10<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?>.allNotNullOrNull(): Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth)
}

// Tuple11

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?): Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any> Tuple11<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?>.allNotNullOrNull(): Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh)
}

// Tuple12

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?): Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any> Tuple12<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?>.allNotNullOrNull(): Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth)
}

// Tuple13

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?): Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any> Tuple13<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?>.allNotNullOrNull(): Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth)
}

// Tuple14

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?, fourteenth: A13?): Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any> Tuple14<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?, A13?>.allNotNullOrNull(): Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    val fourteenth = fourteenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth)
}

// Tuple15

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?, fourteenth: A13?, fifteenth: A14?): Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any> Tuple15<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?, A13?, A14?>.allNotNullOrNull(): Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    val fourteenth = fourteenth ?: return null
    val fifteenth = fifteenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth)
}

// Tuple16

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?, fourteenth: A13?, fifteenth: A14?, sixteenth: A15?): Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any> Tuple16<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?, A13?, A14?, A15?>.allNotNullOrNull(): Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    val fourteenth = fourteenth ?: return null
    val fifteenth = fifteenth ?: return null
    val sixteenth = sixteenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth)
}

// Tuple17

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?, fourteenth: A13?, fifteenth: A14?, sixteenth: A15?, seventeenth: A16?): Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any> Tuple17<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?, A13?, A14?, A15?, A16?>.allNotNullOrNull(): Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    val fourteenth = fourteenth ?: return null
    val fifteenth = fifteenth ?: return null
    val sixteenth = sixteenth ?: return null
    val seventeenth = seventeenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth)
}

// Tuple18

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any, A17 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?, fourteenth: A13?, fifteenth: A14?, sixteenth: A15?, seventeenth: A16?, eighteenth: A17?): Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any, A17 : Any> Tuple18<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?, A13?, A14?, A15?, A16?, A17?>.allNotNullOrNull(): Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    val fourteenth = fourteenth ?: return null
    val fifteenth = fifteenth ?: return null
    val sixteenth = sixteenth ?: return null
    val seventeenth = seventeenth ?: return null
    val eighteenth = eighteenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth)
}

// Tuple19

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any, A17 : Any, A18 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?, fourteenth: A13?, fifteenth: A14?, sixteenth: A15?, seventeenth: A16?, eighteenth: A17?, nineteenth: A18?): Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any, A17 : Any, A18 : Any> Tuple19<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?, A13?, A14?, A15?, A16?, A17?, A18?>.allNotNullOrNull(): Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    val fourteenth = fourteenth ?: return null
    val fifteenth = fifteenth ?: return null
    val sixteenth = sixteenth ?: return null
    val seventeenth = seventeenth ?: return null
    val eighteenth = eighteenth ?: return null
    val nineteenth = nineteenth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth)
}

// Tuple20

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any, A17 : Any, A18 : Any, A19 : Any> allNotNullOrNull(first: A0?, second: A1?, third: A2?, fourth: A3?, fifth: A4?, sixth: A5?, seventh: A6?, eighth: A7?, ninth: A8?, tenth: A9?, eleventh: A10?, twelfth: A11?, thirteenth: A12?, fourteenth: A13?, fifteenth: A14?, sixteenth: A15?, seventeenth: A16?, eighteenth: A17?, nineteenth: A18?, twentieth: A19?): Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>? =
    tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth, twentieth).allNotNullOrNull()

fun <A0 : Any, A1 : Any, A2 : Any, A3 : Any, A4 : Any, A5 : Any, A6 : Any, A7 : Any, A8 : Any, A9 : Any, A10 : Any, A11 : Any, A12 : Any, A13 : Any, A14 : Any, A15 : Any, A16 : Any, A17 : Any, A18 : Any, A19 : Any> Tuple20<A0?, A1?, A2?, A3?, A4?, A5?, A6?, A7?, A8?, A9?, A10?, A11?, A12?, A13?, A14?, A15?, A16?, A17?, A18?, A19?>.allNotNullOrNull(): Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>? {
        val first = first ?: return null
    val second = second ?: return null
    val third = third ?: return null
    val fourth = fourth ?: return null
    val fifth = fifth ?: return null
    val sixth = sixth ?: return null
    val seventh = seventh ?: return null
    val eighth = eighth ?: return null
    val ninth = ninth ?: return null
    val tenth = tenth ?: return null
    val eleventh = eleventh ?: return null
    val twelfth = twelfth ?: return null
    val thirteenth = thirteenth ?: return null
    val fourteenth = fourteenth ?: return null
    val fifteenth = fifteenth ?: return null
    val sixteenth = sixteenth ?: return null
    val seventeenth = seventeenth ?: return null
    val eighteenth = eighteenth ?: return null
    val nineteenth = nineteenth ?: return null
    val twentieth = twentieth ?: return null
    return tupleOf(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth, twentieth)
}
