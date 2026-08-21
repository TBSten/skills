package com.example.compilerpluginsetup

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * IR (backend) extension. Transformations applied here affect the actual
 * bytecode / JS / Native output.
 */
class ExampleIrExtension(
    @Suppress("unused") private val configuration: CompilerConfiguration,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(ExampleTransformer(pluginContext), null)
    }
}
