# Plugin Registration Patterns

Kotlin Compiler Plugin の登録に必要なクラスの設計解説。
**完全なコードは `example/compiler-plugin/src/main/kotlin/com/example/compilerpluginsetup/` 配下の実ファイルが SSoT** (scaffold.sh がコピー・置換する)。

| クラス | 実ファイル | 役割 |
|---|---|---|
| `ExampleCommandLineProcessor` | `example/.../ExampleCommandLineProcessor.kt` | Plugin ID の宣言と CLI オプション定義 |
| `ExampleRegistrar` | `example/.../ExampleRegistrar.kt` | FIR / IR extension の登録 (`supportsK2 = true`) |
| `ExampleIrExtension` | `example/.../ExampleIrExtension.kt` | IR (backend) extension のエントリポイント |
| `ExampleTransformer` | `example/.../ExampleTransformer.kt` | IR 変換本体 (skeleton は no-op) |
| `ExampleFirExtensionRegistrar` | `example/.../fir/ExampleFirExtensionRegistrar.kt` | FIR (frontend) extension の登録 (skeleton は空) |

## CommandLineProcessor

- `pluginId` を宣言し、CLI オプションを定義する
- `@AutoService(CommandLineProcessor::class)` で `META-INF/services` に自動登録される (auto-service-ksp が生成)

### CLI オプションの追加

skeleton には `enabled` オプション 1 つが配線済み (`ExampleCommandLineProcessor` の `CliOption` + `CompilerConfigurationKey` → `ExampleRegistrar` が `false` なら何も登録しない → Gradle DSL `examplePlugin { enabled = false }`)。オプションを増やすときはこの 3 点を同じ形で追加する。

- Gradle plugin が渡す `SubpluginOption` のキーは **全て** `pluginOptions` に宣言する (未宣言だと kotlinc が unknown option で失敗する)
- Gradle 側の渡し方は `gradle-plugin-impl.md` を参照

## CompilerPluginRegistrar

- `ExtensionStorage.registerExtensions` で FIR extension (`FirExtensionRegistrarAdapter.registerExtension`) と IR extension (`IrGenerationExtension.registerExtension`) を登録する
- K2 compiler をサポートする場合は `supportsK2 = true` を設定
- **注意**: `CompilerPluginRegistrar.pluginId` は Kotlin 2.3.0 で abstract メンバーとして追加された。2.3.0 以上では `override val pluginId` が必須、2.2.x 以下では存在しない (override するとコンパイルエラー)。compat module 等で 2.3 未満の compiler に対してコンパイルする場合は外す。値は `CommandLineProcessor.pluginId` と一致させる

### FIR vs IR の使い分け

| Phase | 用途 | 特徴 |
|---|---|---|
| FIR (Frontend) | バリデーション、早期エラー報告 | IDE でリアルタイムエラー表示、正確な行番号 |
| IR (Backend) | コード変換、生成 | 実際のバイトコード/JS/Native 出力に影響 |

- FIR checker はベストエフォート (try-catch で囲む) にし、失敗しても IR phase でフォールバック
- IR extension のみでも compiler plugin は動作する (FIR は任意)

## IrGenerationExtension / Transformer

- `generate()` で `moduleFragment.transform(<Transformer>, null)` を呼び、`IrElementTransformerVoid` 継承の Transformer に変換を実装する
- skeleton の `ExampleTransformer` は no-op。`visitCall` / `visitFunction` 等を override して変換を実装する

## FirExtensionRegistrar

- `configurePlugin()` 内で `+::YourFirCheckersExtension` 形式で FIR extension を登録する
- skeleton の `ExampleFirExtensionRegistrar` は何も登録しない (FIR は任意のため)
