package com.example.snapshot.testing.snapshot.assertion

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.printToString
import io.kotest.assertions.assertSoftly
import io.kotest.core.test.TestScope
import com.example.snapshot.testing.snapshot.internal.RawSnapshotText

private val objectHashPattern = Regex("@[0-9a-fA-F]{4,}")

@OptIn(RawSnapshotText::class)
fun TestScope.shouldMatchSnapshot(
    fileName: String,
    node: SemanticsNodeInteraction,
    maxDepth: Int = 1000,
) = assertSoftly {
    shouldMatchSnapshot(
        fileName = fileName,
        text = node.printToString(maxDepth).replace(objectHashPattern, "@stable"),
        extension = ".txt",
    )

    // レイアウト領域・semantics 情報を視覚画像としてスナップショット
    val layoutImage = renderSemanticsLayout(node)
    shouldMatchSnapshot(fileName = "$fileName-layout", image = layoutImage)
}
