// =============================================================================
// compiler-plugin モジュール: src/main/kotlin/{package}/compiler/PluginRegistrar.kt
// =============================================================================
//
// テンプレート変数:
//   {{PACKAGE}}   - ユーザー指定のパッケージ名 (例: com.example.collector)
//   {{PLUGIN_ID}} - Gradle Plugin ID (例: com.example.collector)

package {{PACKAGE}}.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class CollectorPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(CollectorFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(CollectorIrExtension())
    }
}

// =============================================================================
// META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
// =============================================================================
// ファイル内容 (1行):
// {{PACKAGE}}.compiler.CollectorPluginRegistrar
