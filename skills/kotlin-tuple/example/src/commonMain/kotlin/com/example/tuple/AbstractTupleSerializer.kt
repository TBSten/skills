// NOTE: @file:OptIn must appear before the package declaration.
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
package com.example.tuple

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
    "first", "second", "third", "fourth", "fifth",
    "sixth", "seventh", "eighth", "ninth", "tenth",
    "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth",
    "sixteenth", "seventeenth", "eighteenth", "nineteenth", "twentieth",
)
