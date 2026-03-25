package me.tbsten.simpleloader.simple

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

fun interface SimpleLoaderLogger<in Data> {
    fun on(event: Event<@UnsafeVariance Data>)

    @Serializable
    sealed interface Event<out Data> {
        @Serializable
        data class StateChanged<out Data>(
            val previousState: SimpleLoader.State<Data>,
            val nextState: SimpleLoader.State<Data>,
        ) : Event<Data>

        @Serializable
        data object InitialLoadStarted : Event<Nothing>

        @Serializable
        data object RefreshStarted : Event<Nothing>

        @Serializable
        data class LoadSuccess<out Data>(val data: Data) : Event<Data>

        @Serializable
        data class LoadError(
            @Contextual
            val exception: Throwable,
        ) : Event<Nothing>

        @Serializable
        data class IllegalStateTransition<out Data>(
            val reason: SimpleLoader.IllegalStateTransitionReason<@UnsafeVariance Data>,
        ) : Event<Data>
    }

    companion object {
        fun <Data> noOp(): SimpleLoaderLogger<Data> = SimpleLoaderLogger {}
    }
}
