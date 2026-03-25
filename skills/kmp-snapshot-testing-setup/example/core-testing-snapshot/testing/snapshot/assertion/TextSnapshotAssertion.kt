package com.example.snapshot.testing.snapshot.assertion

import io.kotest.assertions.withClue
import io.kotest.core.test.TestScope
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationStrategy
import com.example.snapshot.testing.snapshot.code.KotlinCodeFormat
import com.example.snapshot.testing.snapshot.internal.RawSnapshotText
import com.example.snapshot.testing.snapshot.internal.buildTextDiffClue
import com.example.snapshot.testing.snapshot.internal.saveActualAndDiffTexts
import com.example.snapshot.testing.snapshot.shouldMatchSnapshot

@RawSnapshotText
fun TestScope.shouldMatchSnapshot(
    fileName: String,
    text: String,
    extension: String = ".txt",
) = shouldMatchSnapshot(
    fileName = fileName,
    extension = extension,
    record = {
        outputStream.bufferedWriter()
            .use { it.append(text) }
    },
    verify = {
        val expected = inputStream.bufferedReader().use { it.readText() }
        if (expected != text) {
            saveActualAndDiffTexts(
                expected = expected,
                actual = text,
                actualFile = actualFile,
                diffFile = diffFile,
            )
            withClue(buildTextDiffClue(expected = expected, actual = text)) {
                expected shouldBe text
            }
        }
    },
)

@OptIn(RawSnapshotText::class)
fun <T> TestScope.shouldMatchSnapshot(
    fileName: String,
    value: T,
    serializer: SerializationStrategy<T>,
    extension: String = ".txt",
) = shouldMatchSnapshot(
    fileName = fileName,
    text = KotlinCodeFormat.encodeToString(serializer, value),
    extension = extension,
)
