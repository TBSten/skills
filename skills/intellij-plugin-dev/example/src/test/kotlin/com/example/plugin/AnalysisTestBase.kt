package com.example.plugin

import com.intellij.openapi.application.runReadAction
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Analysis API 機能テストの harness (references/analysis-api-testing.md の参照実装 / SSoT)。
 * - tearDown を [ignoreUnrelatedLoggedErrors] で包み、統合 IDEA (IU) の bundled plugin
 *   (Vue LSP 等) 由来の無関係な logged error をテスト失敗に昇格させない
 * - テストは EDT なので、AA は `runReadActionBlocking { allowAnalysisOnEdt { analyze(...) } }` で囲む
 *
 * K2 強制は build.gradle.kts の `tasks.test { systemProperty("idea.kotlin.plugin.use.k2", "true") }`
 * と plugin.xml の `<supportsKotlinPluginMode supportsK2="true"/>` (references/setup/basics.md)。
 */
abstract class AnalysisTestBase : BasePlatformTestCase() {

    override fun tearDown() = ignoreUnrelatedLoggedErrors { super.tearDown() }

    /**
     * 既知の無害カテゴリだけ握り潰す。実装由来のエラーは隠さない。
     *
     * NOTE: この substring 一致は spike の最小策で、本番テストには広すぎる
     * (新規の実装エラーの message/stack に `Lsp` 等が偶然含まれるだけで昇格しなくなる)。
     * TODO(CUSTOMIZE): production では logger category の完全一致 + 例外 class + 既知 message
     * prefix の組み合わせへ絞る (references/analysis-api-testing.md「harness の落とし穴」)。
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
                    // 抑制した内容をテスト出力に残す (silent に握り潰さない)
                    println("AnalysisTestBase: suppressed unrelated logged error: category=$category message=$message")
                    emptySet()
                } else {
                    super.processError(category, message, details, t)
                }
            }
        }) { block() }
    }

    /**
     * EDT 上のテストから read action を同期実行する薄い wrapper。
     * (プロダクション側の非同期解析は `ReadAction.nonBlocking()` を使う — ide-integration.md §7)
     */
    protected fun <T> runReadActionBlocking(action: () -> T): T = runReadAction(action)

    companion object {
        /** 既知の無害カテゴリ (IU bundled plugin の初期化失敗 / fixture 終了時の index 掃除) */
        val IGNORABLE_ERROR_PATTERNS: List<String> = listOf("Vue", "Lsp", "stale file ids")
    }
}
