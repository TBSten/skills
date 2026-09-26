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
 *
 * Coordinates / plugin id / supported Kotlin range come from the generated [BuildConfig]
 * (gradle.properties is the SSoT).
 */
class ExampleGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        val extension = target.extensions.create(ExampleExtension.NAME, ExampleExtension::class.java)
        extension.enabled.convention(true)
        extension.addRuntimeDependency.convention(true)

        target.afterEvaluate {
            val hasKotlin = KOTLIN_PLUGIN_IDS.any { target.pluginManager.hasPlugin(it) }
            if (!hasKotlin) {
                // Fail-fast guardrail: without a Kotlin plugin, applyToCompilation is never called
                // and the user would get a silent no-op.
                target.logger.warn(
                    "[example-plugin] ${target.path} applies ${BuildConfig.COMPILER_PLUGIN_ID} but no Kotlin plugin " +
                        "(kotlin-jvm / kotlin-multiplatform / kotlin-android). The compiler plugin has no effect. " +
                        "Apply it to the same module as the Kotlin plugin (applying it to the root project does not " +
                        "propagate to subprojects).",
                )
                return@afterEvaluate
            }
            ExampleKotlinVersionGuard.check(target)
            if (extension.addRuntimeDependency.get()) addRuntimeDependency(target)
        }
    }

    private fun addRuntimeDependency(project: Project) {
        val notation = "${BuildConfig.GROUP_ID}:runtime:${BuildConfig.VERSION}"
        // KMP projects need `commonMainImplementation`; single-target projects use `implementation`.
        val configName =
            if (project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) "commonMainImplementation"
            else "implementation"
        if (project.configurations.findByName(configName) != null) {
            project.dependencies.add(configName, notation)
        } else {
            project.logger.warn(
                "[example-plugin] Could not add the runtime automatically: configuration '$configName' not found. " +
                    "Add it manually: implementation(\"$notation\")",
            )
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.GROUP_ID,
        artifactId = "compiler-plugin",
        version = BuildConfig.VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(ExampleExtension::class.java)
        // Each key must match a CliOption in ExampleCommandLineProcessor.
        return extension.enabled.map { enabled ->
            listOf(SubpluginOption(key = "enabled", value = enabled.toString()))
        }
    }

    private companion object {
        val KOTLIN_PLUGIN_IDS = listOf(
            "org.jetbrains.kotlin.jvm",
            "org.jetbrains.kotlin.multiplatform",
            "org.jetbrains.kotlin.android",
        )
    }
}
