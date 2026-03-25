package com.example.snapshot.testing.property

import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import kotlinx.serialization.KSerializer
import com.example.snapshot.testing.snapshot.code.KotlinCodeFormat

/**
 * 値を [KotlinCodeFormat] でシリアライズし、ラベル付きの `Pair<String, A>` に変換する。
 *
 * ```kotlin
 * Arb.list(Arb.string()).serializer { serializer() }
 * // -> Arb<Pair<String, List<String>>>
 * ```
 */
fun <A> Arb<A>.serializer(serializer: () -> KSerializer<A>): Arb<Pair<String, A>> {
    val s = serializer()
    return map { value -> KotlinCodeFormat.encodeToString(s, value) to value }
}

/**
 * シリアライズ不可能な型向け。[labelOf] でラベルを生成する。
 *
 * ```kotlin
 * Arb.suspendFunction(Arb.list(Arb.string())).withLabel()
 * ```
 */
fun <A> Arb<A>.withLabel(labelOf: (A) -> String = { it.toString() }): Arb<Pair<String, A>> =
    map { value -> labelOf(value) to value }
