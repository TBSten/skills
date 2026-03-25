package me.tbsten.simpleloader.simple

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import me.tbsten.simpleloader.IllegalStateTransitionHandler
import me.tbsten.simpleloader.simple.SimpleLoader.IllegalStateTransitionReason
import me.tbsten.simpleloader.simple.SimpleLoader.State


interface SimpleLoader<out Data> : AutoCloseable {
    val coroutineScope: CoroutineScope
    val state: StateFlow<State<Data>>

    fun load(
        onIllegalStateTransition: (reason: IllegalStateTransitionReason<@UnsafeVariance Data>) -> Unit =
            ::onIllegalStateTransition
    ) = when (val previousState = state.value) {
        is State.Initial,
        is State.InitialError ->
            initialLoad()

        is State.Loaded<*>,
        is State.RefreshError<*> ->
            refresh()

        is State.InitialLoading,
        is State.RefreshLoading<*> ->
            onIllegalStateTransition(
                IllegalStateTransitionReason.CanNotLoadOnLoadingState(previousState),
            )
    }

    fun initialLoad(
        onIllegalStateTransition: (reason: IllegalStateTransitionReason<@UnsafeVariance Data>) -> Unit =
            ::onIllegalStateTransition
    )

    fun refresh(
        onIllegalStateTransition: (reason: IllegalStateTransitionReason<@UnsafeVariance Data>) -> Unit =
            ::onIllegalStateTransition
    )

    fun onIllegalStateTransition(reason: IllegalStateTransitionReason<@UnsafeVariance Data>)

    override fun close() {
        if (coroutineScope.isActive) coroutineScope.cancel()
    }

    interface LoadScope : CoroutineScope

    @Serializable
    sealed interface State<out Data> {
        sealed interface Loading
        sealed interface Error {
            val exception: Throwable
        }

        sealed interface InitialPhase

        sealed interface AfterLoadedPhase<Data> {
            val data: Data
        }

        @Serializable
        data object Initial : State<Nothing>, InitialPhase

        @Serializable
        data object InitialLoading : State<Nothing>, InitialPhase, Loading

        @Serializable
        data class InitialError(
            @Contextual
            override val exception: Throwable,
        ) : State<Nothing>, InitialPhase, Error

        @Serializable
        data class Loaded<Data>(override val data: Data) :
            State<Data>,
            AfterLoadedPhase<Data>

        @Serializable
        data class RefreshLoading<Data>(
            override val data: Data,
        ) : State<Data>, AfterLoadedPhase<Data>, Loading

        @Serializable
        data class RefreshError<Data>(
            override val data: Data,
            @Contextual
            override val exception: Throwable,
        ) : State<Data>, AfterLoadedPhase<Data>, Error
    }

    @Serializable
    sealed interface IllegalStateTransitionReason<Data> {
        val state: State<Data>

        @Serializable
        data class CanNotReloadOnInitialPhase<Data>(override val state: State<Data>) :
            IllegalStateTransitionReason<Data>

        @Serializable
        data class CanNotLoadOnLoadingState<Data>(override val state: State<Data>) :
            IllegalStateTransitionReason<Data>

        @Serializable
        data class InitialLoadOnAfterLoadedPhase<Data>(override val state: State<Data>) :
            IllegalStateTransitionReason<Data>
    }
}

typealias SimpleLoaderIllegalStateTransitionHandler<Data> = (reason: IllegalStateTransitionReason<Data>) -> Unit


internal class SimpleLoaderImpl<out Data>(
    override val coroutineScope: CoroutineScope,
    private val load: suspend SimpleLoader.LoadScope.() -> Data,
    private val onIllegalStateTransition: SimpleLoaderIllegalStateTransitionHandler<Data> = IllegalStateTransitionHandler.default(),
    private val logger: SimpleLoaderLogger<Data> = SimpleLoaderLogger.noOp(),
) : SimpleLoader<Data> {
    private val loadScope = LoadScopeImpl(coroutineScope)

    private val _state =
        MutableStateFlow<State<Data>>(State.Initial)
    override val state = _state.asStateFlow()
    private var loadJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    private fun <S : State<Data>> S.doUpdate(): S = also { nextState ->
        val previousState = _state.value
        _state.update { nextState }
        logger.on(SimpleLoaderLogger.Event.StateChanged(previousState, nextState))
    }

    override fun initialLoad(onIllegalStateTransition: (reason: IllegalStateTransitionReason<@UnsafeVariance Data>) -> Unit) {
        logger.on(SimpleLoaderLogger.Event.InitialLoadStarted)
        loadJob = coroutineScope.launch {
            when (val previousState = state.value) {
                is State.Loading -> {
                    val reason =
                        IllegalStateTransitionReason.CanNotLoadOnLoadingState(previousState)
                    logger.on(SimpleLoaderLogger.Event.IllegalStateTransition(reason))
                    onIllegalStateTransition(reason)
                }

                is State.AfterLoadedPhase<*> -> {
                    val reason =
                        IllegalStateTransitionReason.InitialLoadOnAfterLoadedPhase(previousState)
                    logger.on(SimpleLoaderLogger.Event.IllegalStateTransition(reason))
                    onIllegalStateTransition(reason)
                }

                is State.InitialPhase ->
                    handleInitialLoad()
            }
        }
    }

    private suspend fun handleInitialLoad() {
        State.InitialLoading
            .doUpdate()

        runCatching {
            load.invoke(loadScope)
        }.fold(
            onSuccess = { result ->
                logger.on(SimpleLoaderLogger.Event.LoadSuccess(result))
                State.Loaded(data = result)
            },
            onFailure = { error ->
                logger.on(SimpleLoaderLogger.Event.LoadError(error))
                State.InitialError(exception = error)
            },
        ).doUpdate()
    }

    override fun refresh(onIllegalStateTransition: (reason: IllegalStateTransitionReason<@UnsafeVariance Data>) -> Unit) {
        logger.on(SimpleLoaderLogger.Event.RefreshStarted)
        loadJob = coroutineScope.launch {
            when (val previousState = state.value) {
                is State.RefreshLoading<*> -> {
                    val reason =
                        IllegalStateTransitionReason.CanNotLoadOnLoadingState(previousState)
                    logger.on(SimpleLoaderLogger.Event.IllegalStateTransition(reason))
                    onIllegalStateTransition(reason)
                }

                is State.Initial,
                is State.InitialError,
                is State.InitialLoading -> {
                    val reason =
                        IllegalStateTransitionReason.CanNotReloadOnInitialPhase<Data>(previousState)
                    logger.on(SimpleLoaderLogger.Event.IllegalStateTransition(reason))
                    onIllegalStateTransition(reason)
                }

                is State.AfterLoadedPhase<*> ->
                    @Suppress("UNCHECKED_CAST")
                    handleRefreshLoad(previousState as State.AfterLoadedPhase<Data>)
            }
        }
    }

    private suspend fun handleRefreshLoad(
        previousState: State.AfterLoadedPhase<Data>,
    ) {
        State.RefreshLoading(data = previousState.data)
            .doUpdate()

        runCatching {
            this@SimpleLoaderImpl.load.invoke(loadScope)
        }.fold(
            onSuccess = { result ->
                logger.on(SimpleLoaderLogger.Event.LoadSuccess(result))
                State.Loaded(data = result)
            },
            onFailure = { error ->
                logger.on(SimpleLoaderLogger.Event.LoadError(error))
                State.RefreshError(data = previousState.data, exception = error)
            },
        ).doUpdate()
    }

    override fun onIllegalStateTransition(reason: IllegalStateTransitionReason<@UnsafeVariance Data>): Unit =
        this.onIllegalStateTransition.invoke(reason)

    private class LoadScopeImpl(
        coroutineScope: CoroutineScope,
    ) : CoroutineScope by coroutineScope, SimpleLoader.LoadScope
}

class FakeSimpleLoader<out Data>(
    override val coroutineScope: CoroutineScope,
    override val state: StateFlow<State<Data>> = MutableStateFlow(State.Initial),
    val onInitialLoad: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    private val onIllegalStateTransition: (reason: IllegalStateTransitionReason<Data>) -> Unit =
        { reason -> println("WARN: $reason") },
) : SimpleLoader<Data> {
    constructor(
        coroutineScope: CoroutineScope,
        state: State<Data> = State.Initial,
        onInitialLoad: () -> Unit = {},
        onRefresh: () -> Unit = {},
    ) : this(
        coroutineScope = coroutineScope,
        state = MutableStateFlow(state),
        onInitialLoad = onInitialLoad,
        onRefresh = onRefresh,
    )

    override fun initialLoad(onIllegalStateTransition: (reason: IllegalStateTransitionReason<@UnsafeVariance Data>) -> Unit) =
        onInitialLoad()

    override fun refresh(onIllegalStateTransition: (reason: IllegalStateTransitionReason<@UnsafeVariance Data>) -> Unit) =
        onRefresh()

    override fun onIllegalStateTransition(reason: IllegalStateTransitionReason<@UnsafeVariance Data>): Unit =
        this.onIllegalStateTransition.invoke(reason)
}
