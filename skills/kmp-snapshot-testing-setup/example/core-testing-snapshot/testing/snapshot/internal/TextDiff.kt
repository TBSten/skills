package com.example.snapshot.testing.snapshot.internal

import java.io.File

internal fun buildTextDiffClue(expected: String, actual: String): String {
    val expectedLines = expected.lines()
    val actualLines = actual.lines()
    val maxLen = maxOf(expectedLines.size, actualLines.size)

    // First pass: collect diff info for each line
    data class DiffLine(val type: String, val exp: String?, val act: String?)
    val diffLines = mutableListOf<DiffLine>()

    for (i in 0 until maxLen) {
        val exp = expectedLines.getOrNull(i)
        val act = actualLines.getOrNull(i)
        when {
            exp == act -> diffLines.add(DiffLine("match", exp, null))
            exp == null -> diffLines.add(DiffLine("added", null, act))
            act == null -> diffLines.add(DiffLine("removed", exp, null))
            else -> diffLines.add(DiffLine("changed", exp, act))
        }
    }

    // Second pass: group consecutive diffs and expand changes
    val groups = mutableListOf<List<DiffLine>>()
    var currentGroup = mutableListOf<DiffLine>()

    for (line in diffLines) {
        if (line.type == "match") {
            if (currentGroup.isNotEmpty()) {
                groups.add(currentGroup)
                currentGroup = mutableListOf()
            }
            groups.add(listOf(line))
        } else {
            currentGroup.add(line)
        }
    }
    if (currentGroup.isNotEmpty()) {
        groups.add(currentGroup)
    }

    // Third pass: render with grouped diffs (expected first, then actual)
    return buildString {
        appendLine("Snapshot diff:")
        for (group in groups) {
            if (group.size == 1 && group[0].type == "match") {
                appendLine("   ${group[0].exp}")
            } else {
                // Show all expected values first
                for (line in group) {
                    when (line.type) {
                        "removed" -> appendLine("🟥 ${line.exp}")
                        "changed" -> appendLine("🟥 ${line.exp}")
                    }
                }
                // Then show all actual values
                for (line in group) {
                    when (line.type) {
                        "added" -> appendLine("🟢 ${line.act}")
                        "changed" -> appendLine("🟢 ${line.act}")
                    }
                }
            }
        }
    }
}

internal fun saveActualAndDiffTexts(
    expected: String,
    actual: String,
    actualFile: File,
    diffFile: File,
) {
    actualFile.parentFile.mkdirs()
    actualFile.writeText(actual)

    val expectedLines = expected.lines()
    val actualLines = actual.lines()
    val maxLen = maxOf(expectedLines.size, actualLines.size)
    val diff = buildString {
        for (i in 0 until maxLen) {
            val exp = expectedLines.getOrNull(i)
            val act = actualLines.getOrNull(i)
            when {
                exp == act -> appendLine(" $exp")
                exp == null -> appendLine("+$act")
                act == null -> appendLine("-$exp")
                else -> {
                    appendLine("-$exp")
                    appendLine("+$act")
                }
            }
        }
    }
    diffFile.writeText(diff)
}
