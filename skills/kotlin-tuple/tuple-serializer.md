# TupleSerializer.kt Generation Rules

Requires `kotlinx-serialization` plugin and dependency.

Provides `KSerializer` implementations for Tuple types to support kotlinx.serialization.
Serializes each Tuple as a JSON array (e.g., `[1, "hello", true]`).

**Important**: Tuple2 (= Pair) and Tuple3 (= Triple) already have built-in serializers in kotlinx.serialization, so no custom serializer is needed for them.

## Architecture

Serializers use a two-file structure:

1. **`AbstractTupleSerializer.kt`** — Abstract base class with shared serialize/deserialize logic and `TUPLE_ELEMENT_NAMES` constant.
2. **`TupleSerializer.kt`** — Concrete serializer classes (Tuple0, Tuple1, Tuple4–TupleN) that extend `AbstractTupleSerializer`.

### AbstractTupleSerializer

```kotlin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

    protected abstract fun toValues(value: T): Array<Any?>
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

internal val TUPLE_ELEMENT_NAMES = arrayOf(
    "first", "second", "third", "fourth", "fifth",
    "sixth", "seventh", "eighth", "ninth", "tenth",
    "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth",
    "sixteenth", "seventeenth", "eighteenth", "nineteenth", "twentieth",
)
```

### Concrete Serializers

Tuple0 is a special case (object, no elements). For each other Tuple (Tuple1, Tuple4–TupleN):

```kotlin
// Tuple0 — special case
object Tuple0Serializer : KSerializer<Tuple0> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("Tuple0", StructureKind.LIST)
    override fun serialize(encoder: Encoder, value: Tuple0) {
        encoder.encodeStructure(descriptor) {}
    }
    override fun deserialize(decoder: Decoder): Tuple0 =
        decoder.decodeStructure(descriptor) { Tuple0 }
}

// Tuple4 example — extends AbstractTupleSerializer
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

// ... up to TupleN (skip Tuple2 and Tuple3)
```

**Pattern**: Each `TupleNSerializer` extends `AbstractTupleSerializer`, passing N serializers and slicing `TUPLE_ELEMENT_NAMES`. Only `toValues` and `fromValues` need implementation.
