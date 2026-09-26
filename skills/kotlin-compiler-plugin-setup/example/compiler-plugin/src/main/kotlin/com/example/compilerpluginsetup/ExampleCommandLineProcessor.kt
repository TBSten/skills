package com.example.compilerpluginsetup

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

/**
 * Declares the compiler plugin ID and its CLI options.
 *
 * `@AutoService` (processed by auto-service-ksp) generates the
 * `META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor` entry.
 *
 * Every option the Gradle plugin passes (`SubpluginOption`) must be declared here,
 * otherwise kotlinc fails with an unknown-option error.
 */
@AutoService(CommandLineProcessor::class)
class ExampleCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.example.compilerpluginsetup"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(OPTION_ENABLED)

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            OPTION_ENABLED.optionName -> configuration.put(KEY_ENABLED, value.toBoolean())
            else -> error("Unexpected plugin option: ${option.optionName}")
        }
    }

    companion object {
        val OPTION_ENABLED = CliOption(
            optionName = "enabled",
            valueDescription = "<true|false>",
            description = "Whether the plugin is enabled (default: true)",
            required = false,
        )

        val KEY_ENABLED = CompilerConfigurationKey<Boolean>("enabled")
    }
}
