package com.example.snapshot.ui.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import io.kotest.assertions.assertSoftly
import io.kotest.core.test.TestScope
import com.example.snapshot.testing.snapshot.assertion.shouldMatchSnapshot
import com.example.snapshot.ui.core.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
fun TestScope.runComposableSnapshotTest(
    fileNamePrefix: String = "",
    screenshotFileName: String = "screenshot",
    semanticsFileName: String = "semantics",
    targetNode: ComposeUiTest.() -> SemanticsNodeInteraction = { onRoot() },
    action: suspend ComposeUiTest.() -> Unit,
    content: @Composable () -> Unit,
) {
    runComposeUiTest {
        setContent {
            WithTestGraph {
                AppTheme.Provider {
                    content()
                }
            }
        }

        action()

        assertSoftly {
            shouldMatchSnapshot(fileNamePrefix + screenshotFileName, targetNode().captureToImage())
            shouldMatchSnapshot(fileNamePrefix + semanticsFileName, targetNode())
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun TestScope.runComposableSnapshotTest(
    fileNamePrefix: String = "",
    screenshotFileName: String = "screenshot",
    semanticsFileName: String = "semantics",
    targetNode: ComposeUiTest.() -> SemanticsNodeInteraction = { onRoot() },
    content: @Composable () -> Unit,
) = runComposableSnapshotTest(
    fileNamePrefix = fileNamePrefix,
    screenshotFileName = screenshotFileName,
    semanticsFileName = semanticsFileName,
    targetNode = targetNode,
    content = content,
    action = { },
)
