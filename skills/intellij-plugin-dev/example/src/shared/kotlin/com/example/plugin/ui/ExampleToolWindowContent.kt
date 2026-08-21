package com.example.plugin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text

/**
 * tool window に表示する内容の model。
 * PSI 非依存に保つ (ナビゲーションが要るなら ide-integration.md §4 の SourceAnchor 型を足す)。
 */
data class ExampleModel(
    val title: String,
    val items: List<String>,
)

/**
 * plugin 本体 (bundled Jewel) と preview (standalone Jewel) の両方でコンパイルされる共有
 * Composable (references/setup/preview.md の source set 共有)。両者に存在する Jewel/Compose API
 * だけを使う。ラベルは英語を既定にする (日本語は実測で文字化けしうる — references/gotchas.md)。
 */
@Composable
fun ExampleToolWindowContent(model: ExampleModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(model.title)
        // CUSTOMIZE: 実 UI (図の Compose Canvas / 表など) に置き換える。
        // 行数の多い表は LazyColumn にする (全 row eager compose を避ける — ide-integration.md §7)。
        model.items.forEach { item ->
            Text("- $item")
        }
    }
}
