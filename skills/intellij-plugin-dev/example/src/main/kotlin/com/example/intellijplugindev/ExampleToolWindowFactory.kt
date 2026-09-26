package com.example.intellijplugindev

import com.example.intellijplugindev.ui.ExampleModel
import com.example.intellijplugindev.ui.ExampleToolWindowContent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab

/**
 * Hosts the tool window's Compose (Jewel) UI.
 *
 * The Composable lives in `src/shared` so that the headless preview renders the same code.
 */
internal class ExampleToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab {
            // CUSTOMIZE: build the model from real data. Follow the editor with a debounce and
            // invalidation, and move heavy analysis off the EDT (ReadAction.nonBlocking).
            ExampleToolWindowContent(
                ExampleModel(title = "Example Plugin", items = listOf("Alpha", "Beta", "Gamma")),
            )
        }
    }

    companion object {
        /** Must match the `id` of `<toolWindow>` in plugin.xml. */
        const val TOOL_WINDOW_ID: String = "Example Tool Window"
    }
}
