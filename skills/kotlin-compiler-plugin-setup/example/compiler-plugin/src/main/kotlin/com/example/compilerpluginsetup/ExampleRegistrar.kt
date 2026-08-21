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
 * The plugin ID is declared by [ExampleCommandLineProcessor]; `CompilerPluginRegistrar`
 * itself has no `pluginId` property.
 */
@AutoService(CompilerPluginRegistrar::class)
class ExampleRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // FIR extension (frontend validation, early error reporting)
        FirExtensionRegistrarAdapter.registerExtension(ExampleFirExtensionRegistrar())
        // IR extension (backend code transformation)
        IrGenerationExtension.registerExtension(ExampleIrExtension(configuration))
    }
}
