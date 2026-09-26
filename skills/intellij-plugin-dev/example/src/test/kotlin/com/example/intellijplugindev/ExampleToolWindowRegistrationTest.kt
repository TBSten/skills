package com.example.intellijplugindev

import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowEP

/**
 * plugin.xml registers the tool window with this plugin's factory. Reads the extension point
 * instead of opening the tool window, so it runs headless and fails when the id, the factory or
 * the anchor in plugin.xml drift.
 */
internal class ExampleToolWindowRegistrationTest : AnalysisTestBase() {

    fun `test the tool window is registered on the right with our factory`() {
        val registered = ToolWindowEP.EP_NAME.extensionList.filter { it.id == ExampleToolWindowFactory.TOOL_WINDOW_ID }

        assertEquals("tool windows with id `${ExampleToolWindowFactory.TOOL_WINDOW_ID}`: $registered", 1, registered.size)
        val toolWindow = registered.single()
        assertEquals(ExampleToolWindowFactory::class.java.name, toolWindow.factoryClass)
        assertEquals(ToolWindowAnchor.RIGHT.toString(), toolWindow.anchor)
    }
}
