package com.example.snapshot.testing.snapshot

import io.kotest.core.test.TestScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import com.example.snapshot.ProjectConfig
import com.example.snapshot.testing.snapshot.assertion.shouldMatchSnapshot
import com.example.snapshot.testing.snapshot.code.KotlinCodeFormat

// region Common helpers

internal fun onceAction(action: SnapshotSpecScope.Action): suspend () -> Unit {
    var executed = false
    return {
        if (!executed) {
            executed = true
            action.action()
        }
    }
}

internal fun sequenceAction(actions: List<SnapshotSpecScope.Action>): suspend () -> Unit {
    var executed = false
    return {
        if (!executed) {
            executed = true
            for (action in actions) {
                action.action()
                advanceMainDispatcher()
            }
        }
    }
}

internal fun advanceMainDispatcher() {
    ProjectConfig.instance.testScheduler.advanceUntilIdle()
}

// endregion

// region SnapshotSpecScope

class SnapshotSpecScope(
    coroutineScope: CoroutineScope,
) : CoroutineScope {
    override val coroutineContext =
        coroutineScope.coroutineContext + Dispatchers.Main + kotlinx.coroutines.SupervisorJob()

    companion object

    @PublishedApi
    internal class StateFlowEntry<S>(
        val maxItemCount: Int,
        val name: String,
        val serializer: KSerializer<S>,
        val stateFlow: () -> StateFlow<S>,
    )

    @PublishedApi
    internal class StateEntry<S>(
        val name: String,
        val serializer: KSerializer<S>,
        val state: () -> S,
    )

    internal class Action(val name: String, val action: suspend () -> Unit)

    @PublishedApi
    internal val states = mutableListOf<StateEntry<*>>()

    @PublishedApi
    internal val stateFlows = mutableListOf<StateFlowEntry<*>>()
    internal val actions = mutableListOf<Action>()

    inline fun <reified S> state(name: String, noinline state: () -> S) {
        states.add(StateEntry(name, KotlinCodeFormat.serializersModule.serializer(), state))
    }

    inline fun <reified S> stateFlow(
        name: String,
        maxItemCount: Int = 10,
        noinline stateFlow: () -> StateFlow<S>,
    ) {
        stateFlows.add(
            StateFlowEntry(
                maxItemCount,
                name,
                KotlinCodeFormat.serializersModule.serializer(),
                stateFlow
            )
        )
    }

    fun action(name: String, action: suspend () -> Unit) {
        actions.add(Action(name, action))
    }

    internal suspend fun executeSnapshots(
        testScope: TestScope,
        doAction: suspend () -> Unit,
        fileNamePrefix: String? = null,
    ) {
        fun resolveFileName(name: String): String {
            return if (fileNamePrefix != null) "$fileNamePrefix/$name" else name
        }

        // stateFlow: Channel + Unconfined で全状態遷移を収集 (conflation 回避)
        val stateFlowResults = stateFlows.map { entry ->
            @Suppress("UNCHECKED_CAST")
            val typedEntry = entry as StateFlowEntry<Any?>
            val channel = Channel<Any?>(Channel.UNLIMITED)
            val job = CoroutineScope(Dispatchers.Unconfined).launch {
                typedEntry.stateFlow().collect { channel.send(it) }
            }
            Triple(typedEntry, channel, job)
        }

        doAction()
        advanceMainDispatcher()

        stateFlowResults.forEach { (typedEntry, channel, job) ->
            job.cancel()
            val items = mutableListOf<Any?>()
            while (true) {
                val result = channel.tryReceive()
                if (result.isSuccess) items.add(result.getOrThrow()) else break
            }
            val snapshotItems = items.take(typedEntry.maxItemCount)
            val listSerializer = ListSerializer(typedEntry.serializer)
            testScope.shouldMatchSnapshot(
                fileName = resolveFileName(typedEntry.name),
                value = snapshotItems,
                serializer = listSerializer as SerializationStrategy<Any?>,
            )
        }

        // state: アクション実行後の値をスナップショット
        states.forEach { entry ->
            @Suppress("UNCHECKED_CAST")
            val typedEntry = entry as StateEntry<Any?>
            testScope.shouldMatchSnapshot(
                fileName = resolveFileName(typedEntry.name),
                value = typedEntry.state(),
                serializer = typedEntry.serializer as SerializationStrategy<Any?>,
            )
        }
    }
}

// endregion
