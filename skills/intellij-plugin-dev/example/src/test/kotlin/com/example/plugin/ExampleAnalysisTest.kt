package com.example.plugin

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Analysis API で注釈を解析する機能テストの土台レシピ
 * (references/analysis-api-testing.md「土台のレシピ」の参照実装 / SSoT)。
 *
 * NOTE: BasePlatformTestCase は JUnit3 系なので、メソッド名は `test` prefix 必須
 * (references の snippet に合わせて英語名のまま)。
 * CUSTOMIZE: 注釈名・アサート観点 (initial 解決 / 遷移表 / reachability / ノード→ソース /
 * degraded fallback — analysis-api-testing.md「アサート観点」) を自分のプラグインに合わせる。
 */
@OptIn(KaAllowAnalysisOnEdt::class)
internal class ExampleAnalysisTest : AnalysisTestBase() {

    fun testResolvesAnnotationInitialArgument() {
        // 注釈スタブを fixture 内ソースで同梱 (stdlib の明示追加は不要 — KClass はデフォルト
        // light fixture の stdlib で解決できる)
        myFixture.addFileToProject("com/example/plugin/spec/Annotations.kt", ANNOTATION_STUB)
        val ktFile = myFixture.configureByText("SampleState.kt", SAMPLE_SRC) as KtFile
        val sampleState = ktFile.declarations.filterIsInstance<KtClass>().first { it.name == "SampleState" }

        runReadActionBlocking {        // テストは EDT。analyze は read action 内で
            allowAnalysisOnEdt {       // EDT 上の analyze を許可 (@OptIn(KaAllowAnalysisOnEdt))
                analyze(sampleState) { // KaSession
                    val spec = sampleState.symbol.annotations.firstOrNull {
                        it.classId?.asFqNameString() == "com.example.plugin.spec.ExampleSpec"
                    } ?: error("@ExampleSpec が見つからない")
                    // initial = [X::class] の解決
                    val initialArg = spec.arguments.firstOrNull { it.name.asString() == "initial" }?.expression
                    val names = (initialArg as? KaAnnotationValue.ArrayValue)?.values?.mapNotNull { v ->
                        (v as? KaAnnotationValue.ClassLiteralValue)
                            ?.let { (it.type as? KaClassType)?.classId?.asFqNameString() }
                    }.orEmpty()
                    assertTrue(
                        "initial に Loading が含まれるはず: $names",
                        names.any { it.endsWith("Loading") },
                    )
                }
            }
        }
    }

    companion object {
        /**
         * 注釈スタブ (fixture 内ソース)。FQN を本物と合わせて classId 判定を通す。
         * NOTE: これは AA 解決を実証する最小 spike。production frontend のテストでは、実 runtime
         * 定義と同期した parity fixture を使う (references/analysis-api-testing.md「fixture の注釈スタブ」)。
         */
        private val ANNOTATION_STUB = """
            package com.example.plugin.spec

            import kotlin.reflect.KClass

            annotation class ExampleSpec(val initial: Array<KClass<*>> = [])
        """.trimIndent()

        private val SAMPLE_SRC = """
            package com.example.sample

            import com.example.plugin.spec.ExampleSpec

            sealed interface SampleUiState {
                object Loading : SampleUiState
                data class Loaded(val items: List<String>) : SampleUiState
            }

            @ExampleSpec(initial = [SampleUiState.Loading::class])
            class SampleState
        """.trimIndent()
    }
}
