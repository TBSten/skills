package com.example.compilerpluginsetup

import com.example.compilerpluginsetup.fir.ExampleFirExtensionRegistrar
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/**
 * Registers the FIR / IR extensions of this compiler plugin.
 *
 * `@AutoService` (processed by auto-service-ksp) generates the
 * `META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar` entry.
 *
 * `pluginId` became an abstract member of `CompilerPluginRegistrar` in Kotlin 2.3.0. When you
 * compile against Kotlin < 2.3 (e.g. a compat module), remove the `override` there.
 */
@AutoService(CompilerPluginRegistrar::class)
class ExampleRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "com.example.compilerpluginsetup"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // `enabled = false` (Gradle DSL) -> register nothing, i.e. zero overhead.
        if (configuration.get(ExampleCommandLineProcessor.KEY_ENABLED) == false) return
        // FIR extension (frontend validation, early error reporting)
        FirExtensionRegistrarAdapter.registerExtension(ExampleFirExtensionRegistrar())
        // IR extension (backend code transformation)
        IrGenerationExtension.registerExtension(ExampleIrExtension(configuration))
    }
}
