package io.github.hoge.redact

import io.github.hoge.redact.compat.CompatContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class RedactIrExtension(
    private val messageCollector: MessageCollector,
    private val compat: CompatContext,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach { file ->
            file.declarations.forEach { decl ->
                // @Redact on a non-data class is a user error
                if (compat.hasRedactOnNonDataClass(decl)) {
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "@Redact can only be used in data classes",
                    )
                }
            }
        }
        moduleFragment.transform(RedactToStringTransformer(pluginContext, compat), null)
    }
}
