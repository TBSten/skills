/**
 * Type-safe Tuple data classes for Kotlin.
 *
 * Provides [Tuple0] through [Tuple20] for grouping multiple values with distinct types.
 * [Tuple2] and [Tuple3] are typealiases for [Pair] and [Triple] respectively.
 *
 * @see tupleOf Factory functions to create Tuple instances.
 */
package com.example.tuple

/**
 * A tuple with zero elements.
 */
data object Tuple0 {
    override fun toString(): String = "()"
}

/**
 * A tuple with one element.
 */
data class Tuple1<A0>(
    val first: A0,
) {
    override fun toString(): String = "($first)"
}

/**
 * A tuple with two elements. Typealias for [Pair].
 */
typealias Tuple2<A0, A1> = Pair<A0, A1>

/**
 * A tuple with three elements. Typealias for [Triple].
 */
typealias Tuple3<A0, A1, A2> = Triple<A0, A1, A2>

/**
 * A tuple with four elements.
 */
data class Tuple4<A0, A1, A2, A3>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
) {
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

/**
 * A tuple with five elements.
 */
data class Tuple5<A0, A1, A2, A3, A4>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}

/**
 * A tuple with six elements.
 */
data class Tuple6<A0, A1, A2, A3, A4, A5>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth)"
}

/**
 * A tuple with seven elements.
 */
data class Tuple7<A0, A1, A2, A3, A4, A5, A6>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh)"
}

/**
 * A tuple with eight elements.
 */
data class Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth)"
}

/**
 * A tuple with nine elements.
 */
data class Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth)"
}

/**
 * A tuple with ten elements.
 */
data class Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth)"
}

/**
 * A tuple with eleven elements.
 */
data class Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh)"
}

/**
 * A tuple with twelve elements.
 */
data class Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth)"
}

/**
 * A tuple with thirteen elements.
 */
data class Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth)"
}

/**
 * A tuple with fourteen elements.
 */
data class Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
    val fourteenth: A13,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth, $fourteenth)"
}

/**
 * A tuple with fifteen elements.
 */
data class Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
    val fourteenth: A13,
    val fifteenth: A14,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth, $fourteenth, $fifteenth)"
}

/**
 * A tuple with sixteen elements.
 */
data class Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
    val fourteenth: A13,
    val fifteenth: A14,
    val sixteenth: A15,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth, $fourteenth, $fifteenth, $sixteenth)"
}

/**
 * A tuple with seventeen elements.
 */
data class Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
    val fourteenth: A13,
    val fifteenth: A14,
    val sixteenth: A15,
    val seventeenth: A16,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth, $fourteenth, $fifteenth, $sixteenth, $seventeenth)"
}

/**
 * A tuple with eighteen elements.
 */
data class Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
    val fourteenth: A13,
    val fifteenth: A14,
    val sixteenth: A15,
    val seventeenth: A16,
    val eighteenth: A17,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth, $fourteenth, $fifteenth, $sixteenth, $seventeenth, $eighteenth)"
}

/**
 * A tuple with nineteen elements.
 */
data class Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
    val fourteenth: A13,
    val fifteenth: A14,
    val sixteenth: A15,
    val seventeenth: A16,
    val eighteenth: A17,
    val nineteenth: A18,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth, $fourteenth, $fifteenth, $sixteenth, $seventeenth, $eighteenth, $nineteenth)"
}

/**
 * A tuple with twenty elements.
 */
data class Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>(
    val first: A0,
    val second: A1,
    val third: A2,
    val fourth: A3,
    val fifth: A4,
    val sixth: A5,
    val seventh: A6,
    val eighth: A7,
    val ninth: A8,
    val tenth: A9,
    val eleventh: A10,
    val twelfth: A11,
    val thirteenth: A12,
    val fourteenth: A13,
    val fifteenth: A14,
    val sixteenth: A15,
    val seventeenth: A16,
    val eighteenth: A17,
    val nineteenth: A18,
    val twentieth: A19,
) {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth, $tenth, $eleventh, $twelfth, $thirteenth, $fourteenth, $fifteenth, $sixteenth, $seventeenth, $eighteenth, $nineteenth, $twentieth)"
}
