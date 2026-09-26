package com.example.plugin

import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.application.runReadActionBlocking as platformRunReadActionBlocking

/**
 * Base of the platform tests (BasePlatformTestCase + Analysis API).
 *
 * - `tearDown` runs inside [ignoreUnrelatedLoggedErrors], so that known-harmless errors logged by
 *   plugins bundled in the unified IDEA (the Vue LSP and the like) do not fail the test.
 * - Tests run on the EDT, so wrap the Analysis API in
 *   `runReadActionBlocking { allowAnalysisOnEdt { analyze(...) { } } }`.
 *
 * K2 is forced by the `idea.kotlin.plugin.use.k2` system property of the test task, paired with
 * `<supportsKotlinPluginMode supportsK2="true"/>` in plugin.xml.
 */
abstract class AnalysisTestBase : BasePlatformTestCase() {

    override fun tearDown() = ignoreUnrelatedLoggedErrors { super.tearDown() }

    /**
     * Swallows only the known-harmless categories; errors from this plugin are not hidden.
     *
     * TODO(CUSTOMIZE): the substring match is wide — an error of ours that merely mentions `Lsp`
     *  would be swallowed too. Narrow it to exact logger category + exception class + message prefix.
     */
    protected fun ignoreUnrelatedLoggedErrors(block: () -> Unit) {
        LoggedErrorProcessor.executeWith<Throwable>(object : LoggedErrorProcessor() {
            override fun processError(
                category: String,
                message: String,
                details: Array<out String>,
                t: Throwable?,
            ): Set<Action> {
                val text = "$category $message ${t?.stackTraceToString().orEmpty()}"
                val ignorable = IGNORABLE_ERROR_PATTERNS.any { text.contains(it, ignoreCase = true) }
                return if (ignorable) {
                    // Leave a trace in the test output rather than swallowing it silently.
                    println("AnalysisTestBase: suppressed unrelated logged error: category=$category message=$message")
                    emptySet()
                } else {
                    super.processError(category, message, details, t)
                }
            }
        }) { block() }
    }

    /**
     * Runs a read action synchronously from a test on the EDT. Production code should use
     * `ReadAction.nonBlocking()` instead.
     */
    protected fun <T> runReadActionBlocking(action: () -> T): T = platformRunReadActionBlocking(action)

    companion object {
        /** Bundled plugins of the unified IDEA failing to start, and index cleanup at fixture teardown. */
        val IGNORABLE_ERROR_PATTERNS: List<String> = listOf("Vue", "Lsp", "stale file ids")
    }
}
