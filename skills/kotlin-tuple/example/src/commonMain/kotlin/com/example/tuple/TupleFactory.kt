/**
 * Factory functions for creating Tuple instances.
 *
 * Provides overloaded [tupleOf] functions for 0 to 20 elements.
 *
 * Usage:
 * ```kotlin
 * val empty = tupleOf()                    // Tuple0
 * val single = tupleOf(1)                  // Tuple1<Int>
 * val pair = tupleOf("a", 2)              // Tuple2<String, Int> (= Pair)
 * val quad = tupleOf(1, "a", true, 3.14)  // Tuple4<Int, String, Boolean, Double>
 * ```
 */
package com.example.tuple

/** Creates a [Tuple0] (empty tuple). */
fun tupleOf(): Tuple0 = Tuple0

/** Creates a [Tuple1] with a single element. */
fun <A0> tupleOf(first: A0): Tuple1<A0> = Tuple1(first)

/** Creates a [Tuple2] (= [Pair]) with two elements. */
fun <A0, A1> tupleOf(first: A0, second: A1): Tuple2<A0, A1> = Tuple2(first, second)

fun <A0, A1, A2> tupleOf(first: A0, second: A1, third: A2): Tuple3<A0, A1, A2> = Tuple3(first, second, third)

fun <A0, A1, A2, A3> tupleOf(first: A0, second: A1, third: A2, fourth: A3): Tuple4<A0, A1, A2, A3> =
    Tuple4(first, second, third, fourth)

fun <A0, A1, A2, A3, A4> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4): Tuple5<A0, A1, A2, A3, A4> =
    Tuple5(first, second, third, fourth, fifth)

fun <A0, A1, A2, A3, A4, A5> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5): Tuple6<A0, A1, A2, A3, A4, A5> =
    Tuple6(first, second, third, fourth, fifth, sixth)

fun <A0, A1, A2, A3, A4, A5, A6> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6): Tuple7<A0, A1, A2, A3, A4, A5, A6> =
    Tuple7(first, second, third, fourth, fifth, sixth, seventh)

fun <A0, A1, A2, A3, A4, A5, A6, A7> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7): Tuple8<A0, A1, A2, A3, A4, A5, A6, A7> =
    Tuple8(first, second, third, fourth, fifth, sixth, seventh, eighth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8): Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8> =
    Tuple9(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9): Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> =
    Tuple10(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10): Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> =
    Tuple11(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11): Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> =
    Tuple12(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12): Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> =
    Tuple13(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12, fourteenth: A13): Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> =
    Tuple14(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12, fourteenth: A13, fifteenth: A14): Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> =
    Tuple15(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12, fourteenth: A13, fifteenth: A14, sixteenth: A15): Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> =
    Tuple16(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12, fourteenth: A13, fifteenth: A14, sixteenth: A15, seventeenth: A16): Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> =
    Tuple17(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12, fourteenth: A13, fifteenth: A14, sixteenth: A15, seventeenth: A16, eighteenth: A17): Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> =
    Tuple18(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12, fourteenth: A13, fifteenth: A14, sixteenth: A15, seventeenth: A16, eighteenth: A17, nineteenth: A18): Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> =
    Tuple19(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth)

fun <A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> tupleOf(first: A0, second: A1, third: A2, fourth: A3, fifth: A4, sixth: A5, seventh: A6, eighth: A7, ninth: A8, tenth: A9, eleventh: A10, twelfth: A11, thirteenth: A12, fourteenth: A13, fifteenth: A14, sixteenth: A15, seventeenth: A16, eighteenth: A17, nineteenth: A18, twentieth: A19): Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> =
    Tuple20(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth, thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth, twentieth)
