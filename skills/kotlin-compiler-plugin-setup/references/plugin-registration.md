# Plugin Registration Patterns

Kotlin Compiler Plugin の登録に必要な 2 つのクラスのパターン。

## CommandLineProcessor

Plugin ID を宣言し、CLI オプションを定義する。`@AutoService` で META-INF/services に自動登録される。

```kotlin
package <your-package>

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

@AutoService(CommandLineProcessor::class)
class <YourPlugin>CommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "<your-plugin-id>"
    override val pluginOptions: Collection<AbstractCliOption> = emptyList()
}
```

### CLI オプションの追加

Gradle plugin から compiler plugin に設定を渡す場合:

```kotlin
companion object {
    val OPTION_ENABLED = AbstractCliOption(
        optionName = "enabled",
        valueDescription = "<true|false>",
        description = "Whether the plugin is enabled",
        required = false,
    )
}

override val pluginOptions: Collection<AbstractCliOption> = listOf(OPTION_ENABLED)
```

## CompilerPluginRegistrar

FIR extension と IR extension を登録する。K2 compiler をサポートする場合は `supportsK2 = true` を設定。

```kotlin
package <your-package>

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@AutoService(CompilerPluginRegistrar::class)
class <YourPlugin>Registrar : CompilerPluginRegistrar() {
    override val pluginId: String = "<your-plugin-id>"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // FIR extension (frontend validation, early error reporting)
        FirExtensionRegistrarAdapter.registerExtension(<YourPlugin>FirExtensionRegistrar())
        // IR extension (backend code transformation)
        IrGenerationExtension.registerExtension(<YourPlugin>IrExtension(configuration))
    }
}
```

### FIR vs IR の使い分け

| Phase | 用途 | 特徴 |
|---|---|---|
| FIR (Frontend) | バリデーション、早期エラー報告 | IDE でリアルタイムエラー表示、正確な行番号 |
| IR (Backend) | コード変換、生成 | 実際のバイトコード/JS/Native 出力に影響 |

- FIR checker はベストエフォート (try-catch で囲む) にし、失敗しても IR phase でフォールバック
- IR extension のみでも compiler plugin は動作する (FIR は任意)

## IrGenerationExtension の実装

```kotlin
package <your-package>

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class <YourPlugin>IrExtension(
    private val configuration: CompilerConfiguration,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(<YourPlugin>Transformer(pluginContext), null)
    }
}
```

## FirExtensionRegistrar の実装

```kotlin
package <your-package>.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class <YourPlugin>FirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::< YourPlugin>FirChecker
    }
}
```
