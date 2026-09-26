// =============================================================================
// gradle-plugin モジュール: src/main/kotlin/{package}/gradle/CollectorGradlePlugin.kt
// =============================================================================
//
// テンプレート変数:
//   {{PACKAGE}}              - ユーザー指定のパッケージ名
//   {{PLUGIN_ID}}            - Gradle Plugin ID (例: com.example.collector)
//   {{COMPILER_PLUGIN_ID}}   - Compiler Plugin ID (PluginRegistrar.pluginId と一致)
//   {{ANNOTATIONS_ARTIFACT}} - annotations モジュールの Maven coordinates

package {{PACKAGE}}.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Gradle Plugin: compiler-plugin を Kotlin コンパイルに適用し、
 * annotations モジュールを依存関係に追加する。
 *
 * 使い方:
 * ```kotlin
 * // build.gradle.kts
 * plugins {
 *     id("{{PLUGIN_ID}}")
 * }
 * ```
 */
class CollectorGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        // annotations を implementation 依存に追加
        target.dependencies.add(
            "implementation",
            "{{ANNOTATIONS_ARTIFACT}}",
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "{{COMPILER_PLUGIN_ID}}"

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact(
            groupId = "{{GROUP_ID}}",
            artifactId = "{{COMPILER_PLUGIN_ARTIFACT_ID}}",
            version = "{{VERSION}}",
        )
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }
}

// =============================================================================
// META-INF/gradle-plugins/{{PLUGIN_ID}}.properties
// =============================================================================
// ファイル内容 (1行):
// implementation-class={{PACKAGE}}.gradle.CollectorGradlePlugin
