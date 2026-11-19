package io.github.hoge.specviewer

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

class SpecAnalysisTest : BasePlatformTestCase() {

    fun testResolvesScreenSpec() {
        myFixture.addFileToProject(
            "io/github/hoge/spec/ScreenSpec.kt",
            """
            package io.github.hoge.spec
            annotation class ScreenSpec(val route: String)
            """.trimIndent(),
        )
        val ktFile = myFixture.configureByText(
            "HomeScreen.kt",
            """
            import io.github.hoge.spec.ScreenSpec
            @ScreenSpec(route = "home")
            class HomeScreen
            """.trimIndent(),
        ) as KtFile
        val ktClass = ktFile.declarations.filterIsInstance<KtClass>().first()

        analyze(ktClass) {
            val spec = ktClass.symbol.annotations.firstOrNull {
                it.classId?.asFqNameString() == "io.github.hoge.spec.ScreenSpec"
            }
            assertNotNull(spec)
        }
    }
}
