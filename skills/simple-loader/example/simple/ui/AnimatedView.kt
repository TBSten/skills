package me.tbsten.simpleloader.simple.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.tbsten.simpleloader.simple.SimpleLoader

@Composable
fun <Data> SimpleLoader.State<Data>.AnimatedView(
    onInitialErrorRefresh: () -> Unit,
    initialError: @Composable (SimpleLoader.State.InitialError) -> Unit = {
        it.InitialErrorView(onInitialErrorRefresh)
    },
    initialLoading: @Composable (SimpleLoader.State.InitialLoading) -> Unit = { it.InitialLoadingView() },
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<SimpleLoader.State<Data>>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(220))
            .togetherWith(fadeOut(animationSpec = tween(90)))
            .using(SizeTransform(clip = false))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    label: String = "SimpleLoader.State.AnimatedView",
    contentKey: (targetState: SimpleLoader.State<Data>) -> Any? = { it::class },
    withData: @Composable ViewWithDataScope<Data>.(state: SimpleLoader.State.AfterLoadedPhase<Data>) -> Unit,
) {
    val state = this

    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = transitionSpec,
        contentAlignment = contentAlignment,
        label = label,
        contentKey = contentKey,
    ) { state ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            (state as? SimpleLoader.State.InitialLoading)
                ?.let { initialLoading(it) }

            (state as? SimpleLoader.State.InitialError)
                ?.let { initialError(it) }

            @Suppress("UNCHECKED_CAST")
            (state as? SimpleLoader.State.AfterLoadedPhase<Data>)
                ?.let { state ->
                    withData(
                        object : ViewWithDataScope<Data> {
                            override val state: SimpleLoader.State.AfterLoadedPhase<Data> = state
                        },
                        state,
                    )
                }
        }
    }
}
