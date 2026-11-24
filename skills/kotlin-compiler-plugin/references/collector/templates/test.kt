// =============================================================================
// compiler-plugin モジュール: src/test/kotlin/{package}/compiler/CollectorTest.kt
// =============================================================================
//
// テンプレート変数:
//   {{PACKAGE}}          - ユーザー指定のパッケージ名
//   {{COLLECTABLE_FQN}}  - @Collectable の FQN
//   {{COLLECT_ALL_FQN}}  - @CollectAll の FQN
//
// 依存関係:
//   testImplementation("dev.zacsweers.kctfork:core:<version>")
//   testImplementation("dev.zacsweers.kctfork:ksp:<version>")
//   testImplementation("org.jetbrains.kotlin:kotlin-test")

package {{PACKAGE}}.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.junit.Test
import kotlin.test.assertEquals

class CollectorTest {

    private fun compile(vararg sources: SourceFile): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            this.sources = sources.toList()
            compilerPluginRegistrars = listOf(CollectorPluginRegistrar())
            inheritClassPath = true
        }.compile()
    }

    /**
     * 単一モジュール内で @Collectable + @CollectAll が動作することを確認。
     */
    @Test
    fun `single module collection`() {
        val source = SourceFile.kotlin(
            "test.kt",
            """
            import {{COLLECTABLE_FQN}}
            import {{COLLECT_ALL_FQN}}
            import kotlin.reflect.KClass

            @Collectable(tag = "handler")
            class HandlerA

            @Collectable(tag = "handler")
            class HandlerB

            @CollectAll(tag = "handler")
            fun allHandlers(): List<KClass<*>>

            fun main() {
                val handlers = allHandlers()
                println(handlers) // [class HandlerA, class HandlerB]
            }
            """,
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    /**
     * tag なし (default) で @Collectable が収集されることを確認。
     */
    @Test
    fun `default tag collection`() {
        val source = SourceFile.kotlin(
            "test.kt",
            """
            import {{COLLECTABLE_FQN}}
            import {{COLLECT_ALL_FQN}}
            import kotlin.reflect.KClass

            @Collectable
            class PluginA

            @Collectable
            class PluginB

            @CollectAll
            fun allPlugins(): List<KClass<*>>
            """,
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    /**
     * 異なる tag のクラスが混在しても、正しくフィルタされることを確認。
     */
    @Test
    fun `tag filtering`() {
        val source = SourceFile.kotlin(
            "test.kt",
            """
            import {{COLLECTABLE_FQN}}
            import {{COLLECT_ALL_FQN}}
            import kotlin.reflect.KClass

            @Collectable(tag = "handler")
            class HandlerA

            @Collectable(tag = "service")
            class ServiceA

            @Collectable(tag = "handler")
            class HandlerB

            @CollectAll(tag = "handler")
            fun allHandlers(): List<KClass<*>>

            @CollectAll(tag = "service")
            fun allServices(): List<KClass<*>>
            """,
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    /**
     * @Collectable が 0 件でも @CollectAll がコンパイルエラーにならないことを確認。
     */
    @Test
    fun `empty collection`() {
        val source = SourceFile.kotlin(
            "test.kt",
            """
            import {{COLLECT_ALL_FQN}}
            import kotlin.reflect.KClass

            @CollectAll(tag = "nonexistent")
            fun emptyList(): List<KClass<*>>
            """,
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}
