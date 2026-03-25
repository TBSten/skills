package me.tbsten.simpleloader.simple

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <PartialData, Data, Loader : SimpleLoader<Data>> withPartialData(
    initialPartialData: PartialData,
    loader: WithPartialDataLoaderScope<PartialData>.() -> Loader,
): Pair<Loader, StateFlow<PartialData>> {
    val partialData = MutableStateFlow(initialPartialData)
    val partialDataMutex = Mutex()

    val scope = object : WithPartialDataLoaderScope<PartialData> {
        override suspend fun emitPartialData(data: (PartialData) -> PartialData): PartialData =
            partialDataMutex.withLock {
                val newValue = data(partialData.value)
                partialData.emit(newValue)
                newValue
            }
    }

    val loader = loader(scope)
        .apply {
            coroutineScope.launch {
                state.collect { newState ->
                    when (newState) {
                        is SimpleLoader.State.Loading -> partialDataMutex.withLock {
                            partialData.update { initialPartialData }
                        }

                        else -> Unit
                    }
                }
            }
        }

    return loader to partialData
}

interface WithPartialDataLoaderScope<PartialData> {
    suspend fun emitPartialData(data: (PartialData) -> PartialData): PartialData
    suspend fun emitPartialData(data: PartialData) = emitPartialData { data }
}

suspend inline fun <R> WithPartialDataLoaderScope<Double>.withAutoProgress(
    initialProgress: Double = 0.0,
    interval: Duration = 0.1.seconds,
    progressPerInterval: Double = 5.0,
    maxAutoProgress: Double = 95.0,
    crossinline block: suspend () -> R,
) = coroutineScope {
    val partialLoaderScope = this@withAutoProgress

    partialLoaderScope.emitPartialData { initialProgress }

    val autoProgressJob = launch {
        while (true) {
            delay(interval)
            partialLoaderScope.emitPartialData { minOf(it + progressPerInterval, maxAutoProgress) }
        }
    }
    val blockDeferred = async { block() }

    blockDeferred.await()
        .also { partialLoaderScope.emitPartialData(100.0) }
        .also { autoProgressJob.cancel() }
}
