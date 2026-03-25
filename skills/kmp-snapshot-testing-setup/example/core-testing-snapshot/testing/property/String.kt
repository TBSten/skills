package com.example.snapshot.testing.property

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int

private val DIVERSE_CODEPOINTS: List<IntRange> = listOf(
    0x0020..0x007E, // ASCII printable
    0x3041..0x3096, // ひらがな
    0x30A1..0x30FA, // カタカナ
    0x4E00..0x9FFF, // CJK統合漢字
    0xAC00..0xD7A3, // ハングル
    0x0400..0x04FF, // キリル文字
    0x0600..0x06FF, // アラビア文字
    0x0E01..0x0E3A, // タイ文字
    0x1F600..0x1F64F, // 絵文字 (顔)
    0x1F300..0x1F5FF, // 絵文字 (シンボル)
    0x1F680..0x1F6FF, // 絵文字 (乗り物)
)

private fun Arb.Companion.stringFromCodepoints(
    codepoints: List<IntRange>,
    lengthRange: IntRange = 0..20,
): Arb<String> = arbitrary {
    val length = Arb.int(lengthRange).bind()
    buildString {
        repeat(length) {
            val range = Arb.element(codepoints).bind()
            val codePoint = Arb.int(range).bind()
            appendCodePoint(codePoint)
        }
    }
}

fun Arb.Companion.basicString(lengthRange: IntRange = 0..20): Arb<String> = arbitrary {
    val useSingleCharType = Arb.element(true, false).bind()
    val codepoints = if (useSingleCharType) {
        listOf(Arb.element(DIVERSE_CODEPOINTS).bind())
    } else {
        DIVERSE_CODEPOINTS
    }
    Arb.stringFromCodepoints(codepoints, lengthRange).bind()
}
