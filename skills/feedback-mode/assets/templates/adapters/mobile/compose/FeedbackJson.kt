package com.example.feedback

// 送信 body を作るだけの小さな JSON 書き出し（kotlinx.serialization を足さないため）
internal fun toJson(v: Any?): String = buildString { writeJson(v) }

private fun StringBuilder.writeJson(v: Any?) {
    when (v) {
        null -> append("null")
        is String -> writeString(v)
        is Boolean -> append(v)
        is Float -> append(if (v.isFinite()) v.toString() else "null")
        is Double -> append(if (v.isFinite()) v.toString() else "null")
        is Number -> append(v.toString())
        is Map<*, *> -> {
            append('{')
            var first = true
            for ((k, value) in v) {
                if (!first) append(',')
                first = false
                writeString(k.toString()); append(':'); writeJson(value)
            }
            append('}')
        }
        is Iterable<*> -> {
            append('[')
            v.forEachIndexed { i, e -> if (i > 0) append(','); writeJson(e) }
            append(']')
        }
        else -> writeString(v.toString())
    }
}

private fun StringBuilder.writeString(s: String) {
    append('"')
    for (c in s) when {
        c == '"' -> append("\\\"")
        c == '\\' -> append("\\\\")
        c == '\n' -> append("\\n")
        c == '\r' -> append("\\r")
        c == '\t' -> append("\\t")
        c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
        else -> append(c)
    }
    append('"')
}
