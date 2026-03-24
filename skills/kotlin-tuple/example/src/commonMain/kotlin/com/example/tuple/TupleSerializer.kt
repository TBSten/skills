// NOTE: @file:OptIn must appear before the package declaration.
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
 * Provides: [Tuple0Serializer], [Tuple1Serializer], [Tuple4Serializer]–[Tuple20Serializer].
 *
 * @see AbstractTupleSerializer Common serialization logic shared by all Tuple serializers.
 */
package com.example.tuple

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
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
}

// Tuple2 (= Pair) and Tuple3 (= Triple) have built-in serializers in kotlinx.serialization.

/**
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
}

class Tuple4Serializer<A0, A1, A2, A3>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
) : AbstractTupleSerializer<Tuple4<A0, A1, A2, A3>>(
    "Tuple4", arrayOf(serializer0, serializer1, serializer2, serializer3),
    TUPLE_ELEMENT_NAMES.sliceArray(0..3),
) {
    override fun toValues(value: Tuple4<A0, A1, A2, A3>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple4<A0, A1, A2, A3> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3)
}

class Tuple5Serializer<A0, A1, A2, A3, A4>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>,
) : AbstractTupleSerializer<Tuple5<A0, A1, A2, A3, A4>>(
    "Tuple5", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4),
    TUPLE_ELEMENT_NAMES.sliceArray(0..4),
) {
    override fun toValues(value: Tuple5<A0, A1, A2, A3, A4>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple5<A0, A1, A2, A3, A4> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4)
}

class Tuple6Serializer<A0, A1, A2, A3, A4, A5>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
) : AbstractTupleSerializer<Tuple6<A0, A1, A2, A3, A4, A5>>(
    "Tuple6", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5),
    TUPLE_ELEMENT_NAMES.sliceArray(0..5),
) {
    override fun toValues(value: Tuple6<A0, A1, A2, A3, A4, A5>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple6<A0, A1, A2, A3, A4, A5> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5)
}

class Tuple7Serializer<A0, A1, A2, A3, A4, A5, A6>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>,
) : AbstractTupleSerializer<Tuple7<A0, A1, A2, A3, A4, A5, A6>>(
    "Tuple7", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6),
    TUPLE_ELEMENT_NAMES.sliceArray(0..6),
) {
    override fun toValues(value: Tuple7<A0, A1, A2, A3, A4, A5, A6>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple7<A0, A1, A2, A3, A4, A5, A6> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6)
}

class Tuple8Serializer<A0, A1, A2, A3, A4, A5, A6, A7>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
) : AbstractTupleSerializer<Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>>(
    "Tuple8", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7),
    TUPLE_ELEMENT_NAMES.sliceArray(0..7),
) {
    override fun toValues(value: Tuple8<A0, A1, A2, A3, A4, A5, A6, A7>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple8<A0, A1, A2, A3, A4, A5, A6, A7> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7)
}

class Tuple9Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>,
) : AbstractTupleSerializer<Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>>(
    "Tuple9", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8),
    TUPLE_ELEMENT_NAMES.sliceArray(0..8),
) {
    override fun toValues(value: Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple9<A0, A1, A2, A3, A4, A5, A6, A7, A8> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8)
}

class Tuple10Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
) : AbstractTupleSerializer<Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>>(
    "Tuple10", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9),
    TUPLE_ELEMENT_NAMES.sliceArray(0..9),
) {
    override fun toValues(value: Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple10<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9)
}

class Tuple11Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>,
) : AbstractTupleSerializer<Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>>(
    "Tuple11", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10),
    TUPLE_ELEMENT_NAMES.sliceArray(0..10),
) {
    override fun toValues(value: Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple11<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10)
}

class Tuple12Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
) : AbstractTupleSerializer<Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>>(
    "Tuple12", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11),
    TUPLE_ELEMENT_NAMES.sliceArray(0..11),
) {
    override fun toValues(value: Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple12<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11)
}

class Tuple13Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>,
) : AbstractTupleSerializer<Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>>(
    "Tuple13", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12),
    TUPLE_ELEMENT_NAMES.sliceArray(0..12),
) {
    override fun toValues(value: Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple13<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12)
}

class Tuple14Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>, serializer13: KSerializer<A13>,
) : AbstractTupleSerializer<Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>>(
    "Tuple14", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12, serializer13),
    TUPLE_ELEMENT_NAMES.sliceArray(0..13),
) {
    override fun toValues(value: Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth, value.fourteenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple14<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12, values[13] as A13)
}

class Tuple15Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>, serializer13: KSerializer<A13>,
    serializer14: KSerializer<A14>,
) : AbstractTupleSerializer<Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>>(
    "Tuple15", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12, serializer13, serializer14),
    TUPLE_ELEMENT_NAMES.sliceArray(0..14),
) {
    override fun toValues(value: Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth, value.fourteenth, value.fifteenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple15<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12, values[13] as A13, values[14] as A14)
}

class Tuple16Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>, serializer13: KSerializer<A13>,
    serializer14: KSerializer<A14>, serializer15: KSerializer<A15>,
) : AbstractTupleSerializer<Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>>(
    "Tuple16", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12, serializer13, serializer14, serializer15),
    TUPLE_ELEMENT_NAMES.sliceArray(0..15),
) {
    override fun toValues(value: Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth, value.fourteenth, value.fifteenth, value.sixteenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple16<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12, values[13] as A13, values[14] as A14, values[15] as A15)
}

class Tuple17Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>, serializer13: KSerializer<A13>,
    serializer14: KSerializer<A14>, serializer15: KSerializer<A15>,
    serializer16: KSerializer<A16>,
) : AbstractTupleSerializer<Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>>(
    "Tuple17", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12, serializer13, serializer14, serializer15, serializer16),
    TUPLE_ELEMENT_NAMES.sliceArray(0..16),
) {
    override fun toValues(value: Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth, value.fourteenth, value.fifteenth, value.sixteenth, value.seventeenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple17<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12, values[13] as A13, values[14] as A14, values[15] as A15, values[16] as A16)
}

class Tuple18Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>, serializer13: KSerializer<A13>,
    serializer14: KSerializer<A14>, serializer15: KSerializer<A15>,
    serializer16: KSerializer<A16>, serializer17: KSerializer<A17>,
) : AbstractTupleSerializer<Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>>(
    "Tuple18", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12, serializer13, serializer14, serializer15, serializer16, serializer17),
    TUPLE_ELEMENT_NAMES.sliceArray(0..17),
) {
    override fun toValues(value: Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth, value.fourteenth, value.fifteenth, value.sixteenth, value.seventeenth, value.eighteenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple18<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12, values[13] as A13, values[14] as A14, values[15] as A15, values[16] as A16, values[17] as A17)
}

class Tuple19Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>, serializer13: KSerializer<A13>,
    serializer14: KSerializer<A14>, serializer15: KSerializer<A15>,
    serializer16: KSerializer<A16>, serializer17: KSerializer<A17>,
    serializer18: KSerializer<A18>,
) : AbstractTupleSerializer<Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>>(
    "Tuple19", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12, serializer13, serializer14, serializer15, serializer16, serializer17, serializer18),
    TUPLE_ELEMENT_NAMES.sliceArray(0..18),
) {
    override fun toValues(value: Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth, value.fourteenth, value.fifteenth, value.sixteenth, value.seventeenth, value.eighteenth, value.nineteenth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple19<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12, values[13] as A13, values[14] as A14, values[15] as A15, values[16] as A16, values[17] as A17, values[18] as A18)
}

class Tuple20Serializer<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>(
    serializer0: KSerializer<A0>, serializer1: KSerializer<A1>,
    serializer2: KSerializer<A2>, serializer3: KSerializer<A3>,
    serializer4: KSerializer<A4>, serializer5: KSerializer<A5>,
    serializer6: KSerializer<A6>, serializer7: KSerializer<A7>,
    serializer8: KSerializer<A8>, serializer9: KSerializer<A9>,
    serializer10: KSerializer<A10>, serializer11: KSerializer<A11>,
    serializer12: KSerializer<A12>, serializer13: KSerializer<A13>,
    serializer14: KSerializer<A14>, serializer15: KSerializer<A15>,
    serializer16: KSerializer<A16>, serializer17: KSerializer<A17>,
    serializer18: KSerializer<A18>, serializer19: KSerializer<A19>,
) : AbstractTupleSerializer<Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>>(
    "Tuple20", arrayOf(serializer0, serializer1, serializer2, serializer3, serializer4, serializer5, serializer6, serializer7, serializer8, serializer9, serializer10, serializer11, serializer12, serializer13, serializer14, serializer15, serializer16, serializer17, serializer18, serializer19),
    TUPLE_ELEMENT_NAMES.sliceArray(0..19),
) {
    override fun toValues(value: Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>): Array<Any?> =
        arrayOf(value.first, value.second, value.third, value.fourth, value.fifth, value.sixth, value.seventh, value.eighth, value.ninth, value.tenth, value.eleventh, value.twelfth, value.thirteenth, value.fourteenth, value.fifteenth, value.sixteenth, value.seventeenth, value.eighteenth, value.nineteenth, value.twentieth)

    @Suppress("UNCHECKED_CAST")
    override fun fromValues(values: Array<Any?>): Tuple20<A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> =
        tupleOf(values[0] as A0, values[1] as A1, values[2] as A2, values[3] as A3, values[4] as A4, values[5] as A5, values[6] as A6, values[7] as A7, values[8] as A8, values[9] as A9, values[10] as A10, values[11] as A11, values[12] as A12, values[13] as A13, values[14] as A14, values[15] as A15, values[16] as A16, values[17] as A17, values[18] as A18, values[19] as A19)
}
