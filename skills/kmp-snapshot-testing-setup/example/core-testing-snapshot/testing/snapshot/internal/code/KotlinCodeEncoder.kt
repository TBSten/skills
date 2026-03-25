package com.example.snapshot.testing.snapshot.internal.code

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Kotlin コード形式でシリアライズするカスタムエンコーダ。
 * Kotlin の標準ライブラリ関数形式で出力。
 *
 * 例:
 * - Loaded(data = "x") → Loaded(\n  data = "x",\n)
 * - listOf("a", "b") → listOf(\n  "a",\n  "b",\n)
 * - mapOf(...) → mapOf(\n  "key" to "value",\n)
 */
@OptIn(ExperimentalSerializationApi::class)
internal class KotlinCodeEncoder(
    override val serializersModule: SerializersModule,
) : AbstractEncoder() {
    private val sb = StringBuilder()
    private var indent = 0
    private val structureStack = mutableListOf<StructureState>()
    private val elementStack =
        mutableListOf<Boolean>()  // Track if element was encoded for each level
    private var isMapKeyFinished = false

    sealed class StructureState {
        data class ClassState(val className: String) : StructureState()
        object ListState : StructureState()
        object ObjectState : StructureState()
        object MapState : StructureState()
        object PolymorphicState : StructureState()
    }

    fun result(): String = sb.toString()

    private fun push(state: StructureState) {
        structureStack.add(state)
        elementStack.add(false)  // Initially, no element encoded at this level
    }

    private fun pop(): StructureState {
        elementStack.removeAt(elementStack.size - 1)
        return structureStack.removeAt(structureStack.size - 1)
    }

    private fun markElementEncoded() {
        if (elementStack.isNotEmpty()) {
            elementStack[elementStack.size - 1] = true
        }
    }

    private fun hasElementAtCurrentLevel(): Boolean = elementStack.lastOrNull() ?: false

    private fun indentStr(): String = "  ".repeat(indent)

    /**
     * 値出力後に呼び出す。
     * MAP: key と value を " to " で分ける、エントリごとに ",\n"
     * CLASS/LIST: ",\n" を追記
     */
    private fun appendValueEnd() {
        val currentStructure = structureStack.lastOrNull { it !is StructureState.PolymorphicState }
        when (currentStructure) {
            StructureState.MapState -> {
                if (isMapKeyFinished) {
                    // Just finished value, append comma and newline
                    sb.append(",\n")
                    isMapKeyFinished = false
                } else {
                    // Just finished key, append " to " separator
                    sb.append(" to ")
                    isMapKeyFinished = true
                }
            }

            is StructureState.ClassState, StructureState.ListState -> {
                sb.append(",\n")
            }

            else -> {}
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        // Extract full nested class name (e.g., "TopLevelClass.InnerClass1.InnerClass2")
        // Remove package prefix (lowercase parts) and keep only class names (uppercase parts)
        val parts = descriptor.serialName.split(".")
        val fullName = parts.dropWhile { it.isNotEmpty() && it[0].isLowerCase() }.joinToString(".")

        when (descriptor.kind) {
            StructureKind.OBJECT -> {
                sb.append(fullName)
                push(StructureState.ObjectState)
            }

            StructureKind.CLASS -> {
                sb.append("$fullName(")
                push(StructureState.ClassState(fullName))
                indent++
            }

            StructureKind.LIST -> {
                // Distinguish between List and Set based on serialName
                val functionName = if (descriptor.serialName.contains("Set")) "setOf" else "listOf"
                sb.append("$functionName(")
                push(StructureState.ListState)
                indent++
            }

            StructureKind.MAP -> {
                sb.append("mapOf(")
                push(StructureState.MapState)
                indent++
            }

            is PolymorphicKind -> {
                push(StructureState.PolymorphicState)
            }

            else -> {}
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val hasElement = hasElementAtCurrentLevel()
        when (pop()) {
            StructureState.ObjectState -> appendValueEnd()
            is StructureState.ClassState -> {
                indent--
                if (hasElement) {
                    sb.append("${indentStr()})")
                } else {
                    sb.append(")")
                }
                appendValueEnd()
            }

            StructureState.ListState -> {
                indent--
                if (hasElement) {
                    sb.append("${indentStr()})")
                } else {
                    // Empty collection - replace "listOf(" or "setOf(" with "emptyList()" or "emptySet()"
                    val lastListOf = sb.lastIndexOf("listOf(")
                    val lastSetOf = sb.lastIndexOf("setOf(")

                    if (lastSetOf > lastListOf && lastSetOf >= 0) {
                        // It was a setOf - replace "setOf(" with "emptySet()"
                        sb.replace(lastSetOf, lastSetOf + 6, "emptySet()")
                    } else if (lastListOf >= 0) {
                        // It was a listOf - replace "listOf(" with "emptyList()"
                        sb.replace(lastListOf, lastListOf + 7, "emptyList()")
                    }
                }
                appendValueEnd()
            }

            StructureState.MapState -> {
                indent--
                if (hasElement) {
                    sb.append("${indentStr()})")
                } else {
                    // Empty map - replace "mapOf(" with "emptyMap()"
                    val lastOpenParen = sb.lastIndexOf("mapOf(")
                    if (lastOpenParen >= 0) {
                        sb.replace(lastOpenParen, lastOpenParen + 6, "emptyMap()")
                    }
                }
                appendValueEnd()
                isMapKeyFinished = false
            }

            StructureState.PolymorphicState -> {
                // nothing to do
            }
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return when (descriptor.kind) {
            is PolymorphicKind -> {
                // skip type identifier at index 0
                index != 0
            }

            StructureKind.CLASS -> {
                if (index == 0 && !hasElementAtCurrentLevel()) {
                    sb.append("\n")
                }
                markElementEncoded()
                sb.append("${indentStr()}${descriptor.getElementName(index)} = ")
                true
            }

            StructureKind.LIST -> {
                if (index == 0 && !hasElementAtCurrentLevel()) {
                    sb.append("\n")
                }
                markElementEncoded()
                sb.append(indentStr())
                true
            }

            StructureKind.MAP -> {
                if (index == 0 && !hasElementAtCurrentLevel()) {
                    sb.append("\n")
                }
                markElementEncoded()
                // For maps, index is alternating: 0=key1, 1=value1, 2=key2, 3=value2, etc.
                if (index % 2 == 0) {
                    // Start of new entry
                    sb.append(indentStr())
                }
                true
            }

            else -> true
        }
    }

    override fun encodeNull() {
        sb.append("null")
        appendValueEnd()
    }

    override fun encodeBoolean(value: Boolean) {
        sb.append(value.toString())
        appendValueEnd()
    }

    override fun encodeByte(value: Byte) {
        sb.append(value.toString())
        appendValueEnd()
    }

    override fun encodeShort(value: Short) {
        sb.append(value.toString())
        appendValueEnd()
    }

    override fun encodeInt(value: Int) {
        sb.append(value.toString())
        appendValueEnd()
    }

    override fun encodeLong(value: Long) {
        sb.append("${value}L")
        appendValueEnd()
    }

    override fun encodeFloat(value: Float) {
        sb.append("${value}f")
        appendValueEnd()
    }

    override fun encodeDouble(value: Double) {
        sb.append(value.toString())
        appendValueEnd()
    }

    override fun encodeChar(value: Char) {
        sb.append("'${value.escape()}'")
        appendValueEnd()
    }

    override fun encodeString(value: String) {
        sb.append("\"${value.escape()}\"")
        appendValueEnd()
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        val enumName = enumDescriptor.serialName.substringAfterLast(".")
        val valueName = enumDescriptor.getElementName(index)
        sb.append("$enumName.$valueName")
        appendValueEnd()
    }

    private fun String.escape(): String = buildString {
        for (char in this@escape) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    private fun Char.escape(): String = when (this) {
        '\\' -> "\\\\"
        '\'' -> "\\'"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        else -> toString()
    }
}
