package me.tbsten.simpleloader.simple.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.tbsten.simpleloader.simple.SimpleLoader

@Composable
inline fun <Data> SimpleLoader.State<Data>.View(
    initialError: @Composable SimpleLoader.State.InitialError.() -> Unit = {},
    initialLoading: @Composable SimpleLoader.State.InitialLoading.() -> Unit = { InitialLoadingView() },
    withData: @Composable ViewWithDataScope<Data>.(state: SimpleLoader.State.AfterLoadedPhase<Data>) -> Unit,
) {
    val state = this

    (state as? SimpleLoader.State.InitialLoading)
        ?.initialLoading()

    (state as? SimpleLoader.State.InitialError)?.let {
        it.initialError()
    }

    @Suppress("UNCHECKED_CAST")
    (state as? SimpleLoader.State.AfterLoadedPhase<Data>)?.let { state ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            withData(
                object : ViewWithDataScope<Data> {
                    override val state: SimpleLoader.State.AfterLoadedPhase<Data> = state
                },
                state,
            )
        }
    }
}

interface ViewWithDataScope<Data> {
    val state: SimpleLoader.State.AfterLoadedPhase<Data>

    val isRefreshLoading: Boolean
        get() = state is SimpleLoader.State.RefreshLoading<*>

    val isRefreshError: Boolean
        get() = state is SimpleLoader.State.RefreshError<*>
}
