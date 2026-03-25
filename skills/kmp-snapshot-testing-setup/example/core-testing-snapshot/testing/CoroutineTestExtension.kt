package com.example.snapshot.testing

import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.BeforeProjectListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
object CoroutineTestExtension : BeforeProjectListener, AfterProjectListener {

    private val dispatcher = StandardTestDispatcher()

    override suspend fun beforeProject() {
        Dispatchers.setMain(dispatcher)
    }

    override suspend fun afterProject() {
        Dispatchers.resetMain()
    }
}
