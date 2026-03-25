package com.example.snapshot.testing.snapshot.internal.code

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * スナップショットテスト用の Throwable シリアライザ。
 * "クラス名: メッセージ" 形式の文字列としてシリアライズする。
 */
object ThrowableSerializerForKotlinCodeFormat : KSerializer<Throwable> {
    override val descriptor = PrimitiveSerialDescriptor("Throwable", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Throwable) {
        encoder.encodeString("${value::class.simpleName}: ${value.message}")
    }

    override fun deserialize(decoder: Decoder): Throwable {
        return RuntimeException(decoder.decodeString())
    }
}
