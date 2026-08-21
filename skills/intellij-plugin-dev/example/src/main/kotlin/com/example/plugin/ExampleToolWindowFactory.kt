package com.example.plugin

import com.example.plugin.ui.ExampleModel
import com.example.plugin.ui.ExampleToolWindowContent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab

/**
 * tool window に Compose (Jewel) UI をホストする (references/ide-integration.md §1)。
 * `addComposeTab` は内部で `JewelComposePanel` + `enableNewSwingCompositing`。
 * この中の Composable (src/shared) を preview (`PreviewMain`) と共有するので、
 * headless PNG は出荷物に忠実になる (references/headless-preview.md)。
 */
internal class ExampleToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Example") {
            // CUSTOMIZE: 実データから model を組み立てて渡す。
            // エディタ追従 (選択・編集の反映) を足すなら ide-integration.md §3 (デバウンス +
            // invalidation)、非同期解析は §6 (lifecycle) / §7 (性能・cancellation) を参照。
            ExampleToolWindowContent(
                ExampleModel(title = "Example", items = listOf("Alpha", "Beta", "Gamma")),
            )
        }
    }
}
