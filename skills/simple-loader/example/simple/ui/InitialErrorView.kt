package me.tbsten.simpleloader.simple.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.tbsten.simpleloader.simple.SimpleLoader

@Composable
fun SimpleLoader.State.InitialError.InitialErrorView(
    onRefresh: () -> Unit,
    refreshButtonText: String = "Retry",
    modifier: Modifier = Modifier,
) {
    InitialErrorViewInternal(
        message = this.exception.message ?: "An unknown error occurred",
        onRefresh = onRefresh,
        refreshButtonText = refreshButtonText,
        modifier = modifier,
    )
}

@Composable
private fun InitialErrorViewInternal(
    message: String,
    onRefresh: () -> Unit,
    refreshButtonText: String = "Retry",
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(message)

        Button(onClick = onRefresh) {
            Text(refreshButtonText)
        }
    }
}
