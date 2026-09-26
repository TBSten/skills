package com.example.intellijplugindev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text

/**
 * What the tool window shows. Keep it free of PSI so that the preview can build it; add a
 * source-anchor type if nodes need to navigate to code.
 */
data class ExampleModel(
    val title: String,
    val items: List<String>,
)

/**
 * Compiled into both the plugin (bundled Jewel) and the preview (standalone Jewel), so it may
 * only use Jewel/Compose API present in both.
 */
@Composable
fun ExampleToolWindowContent(model: ExampleModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(model.title)
        // CUSTOMIZE: replace with the real UI. Use LazyColumn for long lists.
        model.items.forEach { item ->
            Text("- $item")
        }
    }
}
