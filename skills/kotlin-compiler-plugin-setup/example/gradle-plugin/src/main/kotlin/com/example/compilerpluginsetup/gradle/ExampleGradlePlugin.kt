package com.example.compilerpluginsetup.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Wraps the compiler plugin as a Gradle plugin so users only need
 * `plugins { id("com.example.compilerpluginsetup") }`.
 */
class ExampleGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        // Add the runtime dependency automatically.
        // KMP projects need `commonMainImplementation`; single-target projects use `implementation`.
        target.afterEvaluate {
            val hasKmpPlugin = target.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
            val configName = if (hasKmpPlugin) "commonMainImplementation" else "implementation"
            target.dependencies.add(
                configName,
                "com.example.compilerpluginsetup:runtime:$VERSION",
            )
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "com.example.compilerpluginsetup"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.example.compilerpluginsetup",
        artifactId = "compiler-plugin",
        version = VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> =
        // To pass options, read your Gradle extension here and map it to SubpluginOption:
        // listOf(SubpluginOption(key = "enabled", value = extension.enabled.get().toString()))
        kotlinCompilation.target.project.provider { emptyList() }

    companion object {
        // TODO: Keep in sync with the published artifact version.
        private const val VERSION = "0.1.0"
    }
}
