package com.example.snapshot.testing.snapshot.code

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import com.example.snapshot.testing.snapshot.internal.code.ThrowableSerializerForKotlinCodeFormat
import com.example.snapshot.testing.snapshot.internal.code.KotlinCodeEncoder

/**
 * Kotlin コンストラクタ呼び出し形式でシリアライズする [StringFormat]。
 *
 * 例:
 * - `Loaded(data = "x",)`
 * - `["a", "b"]`
 * - `null`
 */
val KotlinCodeFormat: StringFormat = object : StringFormat {
    override val serializersModule: SerializersModule = SerializersModule {
        contextual(ThrowableSerializerForKotlinCodeFormat)
    }

    override fun <T> encodeToString(
        serializer: SerializationStrategy<T>,
        value: T,
    ): String {
        val encoder = KotlinCodeEncoder(serializersModule)
        encoder.encodeSerializableValue(serializer, value)
        return encoder.result()
    }

    override fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        string: String,
    ): T = throw UnsupportedOperationException("KotlinCodeFormat does not support decoding")
}
