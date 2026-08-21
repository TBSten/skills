package com.example.compilerpluginsetup

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

/**
 * Declares the compiler plugin ID and its CLI options.
 *
 * `@AutoService` (processed by auto-service-ksp) generates the
 * `META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor` entry.
 */
@AutoService(CommandLineProcessor::class)
class ExampleCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.example.compilerpluginsetup"

    override val pluginOptions: Collection<AbstractCliOption> = emptyList()

    // To pass settings from the Gradle plugin, declare options like this and
    // return them from `pluginOptions`:
    //
    // companion object {
    //     val OPTION_ENABLED = AbstractCliOption(
    //         optionName = "enabled",
    //         valueDescription = "<true|false>",
    //         description = "Whether the plugin is enabled",
    //         required = false,
    //     )
    // }
}
