package me.tbsten.simpleloader.simple

import kotlinx.coroutines.CoroutineScope
import me.tbsten.simpleloader.IllegalStateTransitionHandler

interface SimpleLoaderFactory {
    fun <Data> defaultOnIllegalStateTransition(): SimpleLoaderIllegalStateTransitionHandler<Data>
    fun <Data> defaultLogger(): SimpleLoaderLogger<Data>

    fun <Data> create(
        coroutineScope: CoroutineScope,
        onIllegalStateTransition: SimpleLoaderIllegalStateTransitionHandler<Data> = defaultOnIllegalStateTransition(),
        logger: SimpleLoaderLogger<Data> = defaultLogger(),
        load: suspend SimpleLoader.LoadScope.() -> Data,
    ): SimpleLoader<Data> = SimpleLoaderImpl(
        coroutineScope = coroutineScope,
        onIllegalStateTransition = onIllegalStateTransition,
        logger = logger,
        load = load,
    )

    companion object {
        val Default: SimpleLoaderFactory = object : SimpleLoaderFactory {
            override fun <Data> defaultOnIllegalStateTransition(): SimpleLoaderIllegalStateTransitionHandler<Data> =
                IllegalStateTransitionHandler.default()

            override fun <Data> defaultLogger(): SimpleLoaderLogger<Data> =
                SimpleLoaderLogger.noOp()
        }
    }
}
