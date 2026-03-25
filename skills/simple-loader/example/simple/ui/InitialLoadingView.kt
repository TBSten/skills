package me.tbsten.simpleloader.simple.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import me.tbsten.simpleloader.simple.SimpleLoader
import kotlin.time.Duration.Companion.seconds

@Composable
fun SimpleLoader.State.InitialLoading.InitialLoadingView(
    modifier: Modifier = Modifier,
) {
    InitialLoadingViewInternal(
        modifier = modifier,
    )
}

@Composable
private fun InitialLoadingViewInternal(
    modifier: Modifier = Modifier,
) {
    var visibleIndicator by rememberSaveable { mutableStateOf(false) }.also { state ->
        LaunchedEffect(Unit) {
            delay(1.seconds)
            state.value = true
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        if (visibleIndicator) {
            CircularProgressIndicator()
        }
    }
}
