package com.example.snapshot

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.htmlreporter.HtmlReporter
import io.kotest.extensions.junitxml.JunitXmlReporter
import io.kotest.property.PropertyTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import com.example.snapshot.testing.snapshot.internal.OrphanedSnapshotDetector

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectConfig : AbstractProjectConfig() {
    private val testDispatcher = StandardTestDispatcher()
    val testScheduler: TestCoroutineScheduler get() = testDispatcher.scheduler

    companion object {
        /** テスト中にスケジューラへアクセスするためのインスタンス参照。 */
        lateinit var instance: ProjectConfig
            private set
    }

    override suspend fun beforeProject() {
        instance = this
        Dispatchers.setMain(testDispatcher)
        PropertyTesting.defaultIterationCount =
            System.getProperty("pbt.iteration.count")?.toIntOrNull() ?: 2000
    }

    override suspend fun afterProject() {
        Dispatchers.resetMain()
    }

    override val extensions: List<Extension> = listOf(
        JunitXmlReporter(
            includeContainers = false,
            useTestPathAsName = true,
        ),
        HtmlReporter(),
        OrphanedSnapshotDetector(),
    )
}
