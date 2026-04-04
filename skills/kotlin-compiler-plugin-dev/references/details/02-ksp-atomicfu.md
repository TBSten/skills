# KSP / atomicfu 詳細

## KSP

### KSP K1 (compiler-plugin)

#### 登録エントリポイント
- ファイル: `https://github.com/google/ksp/blob/main/compiler-plugin/src/main/kotlin/com/google/devtools/ksp/KotlinSymbolProcessingPlugin.kt`
- 登録内容:
  - `KotlinSymbolProcessingCommandLineProcessor` : CLI オプション処理
  - `KotlinSymbolProcessingComponentRegistrar` : `ComponentRegistrar` 実装。`AnalysisHandlerExtension` として `KotlinSymbolProcessingExtension` を登録。`DualLookupTracker` も登録する。
  - K2 (languageVersion >= 2.0) の場合は登録をスキップする（K1 専用）。ただし `supportsK2 = true` を返してコンパイラのエラーを抑制。

### 各機能の詳細

#### `KotlinSymbolProcessingCommandLineProcessor` (K1)
- **継承/実装**: `CommandLineProcessor`
- **オーバーライドメソッド**:
  - `pluginId`: `"com.google.devtools.ksp.symbol-processing"`
  - `pluginOptions`: `KspCliOption.values()` を全列挙
  - `processOption()`: 受け取ったオプションを `KspOptions.Builder` に蓄積し `configuration[KSP_OPTIONS]` に格納
- **動作の詳細**: コンパイラ起動時のコマンドライン引数を KspOptions に変換する責務のみ。

#### `KotlinSymbolProcessingComponentRegistrar` (K1)
- **継承/実装**: `ComponentRegistrar`（deprecated API）
- **オーバーライドメソッド**:
  - `registerProjectComponents(project, configuration)`: K2 時はスキップ。`KotlinSymbolProcessingExtension` を `AnalysisHandlerExtension.registerExtension()` で登録。`DualLookupTracker` を `CommonConfigurationKeys.LOOKUP_TRACKER` に設定。
  - `supportsK2`: `true`（K2 でのコンパイラエラー抑止のため）
- **動作の詳細**: `AnalysisHandlerExtension` を Intellij DI コンテナに登録することで、型解析フェーズにフックする。

#### `AbstractKotlinSymbolProcessingExtension` / `KotlinSymbolProcessingExtension` (K1 / FIR前 / AnalysisHandler)
- **ファイル**: `https://github.com/google/ksp/blob/main/compiler-plugin/src/main/kotlin/com/google/devtools/ksp/KotlinSymbolProcessingExtension.kt`
- **継承/実装**: `AnalysisHandlerExtension`
- **オーバーライドメソッド**:
  - `doAnalysis(project, module, projectContext, files, bindingTrace, componentProvider): AnalysisResult?`
- **動作の詳細**:
  1. `initialized` フラグで初回ラウンドと2ラウンド目以降を区別。
  2. Kotlin ソース (`KtFile`) と Java ソース (`PsiJavaFile`) を収集し `KSFile` のリストに変換。
  3. `IncrementalContext.calcDirtyFiles()` でダーティファイルを計算。
  4. 初回のみ `CodeGeneratorImpl` と各 `SymbolProcessor` を `SymbolProcessorProvider.create()` で生成。
  5. 各ラウンドで `processor.process(resolver)` を呼び出し、defer されたシンボルを蓄積。
  6. 生成ファイルが 0 なら `finished = true` にしてラストラウンドとして判断。
  7. エラー時は `processor.onError()`、完了時は `processor.finish()` を呼ぶ。
  8. 通常終了 (`withCompilation=false`): `AnalysisResult.success(shouldGenerateCode=false)` を返す。`withCompilation=true`: `AnalysisResult.RetryWithAdditionalRoots` でさらに追加ソースを Kotlin コンパイラに渡す。
  9. `loadProviders()` は `URLClassLoader` + `ServiceLoaderLite` で `SymbolProcessorProvider` の実装を動的ロード。
- **注意点**: Java ファイルは `byRounds/<n>/` というシャドウディレクトリ経由で管理される。最終ラウンド後に `updateFromShadow()` で実際の出力先にコピー。

---

### KSP K2 (kotlin-analysis-api)

#### 登録エントリポイント
- ファイル: `https://github.com/google/ksp/blob/main/kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/KSPLoader.kt`
- 登録内容:
  - `KSPLoader.loadAndRunKSP()` : Gradle タスク (`KspAATask`) から呼ばれる JVM スタティックメソッド。`KSPConfig` をデシリアライズして `KotlinSymbolProcessing` を実行する。
  - K2 は compiler plugin ではなく **スタンドアロン Analysis API セッション**として動作する。`AnalysisHandlerExtension` は使わない。

#### `KotlinSymbolProcessing` (K2)
- **ファイル**: `https://github.com/google/ksp/blob/main/kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/KotlinSymbolProcessing.kt`
- **継承/実装**: なし（独立したクラス）
- **主要メソッド**:
  - `execute(): ExitCode` : 処理のメインループ
  - `createAASession()`: `StandaloneAnalysisAPISession` を構築。`KtModuleProviderBuilder` でプラットフォーム（JVM/JS/Native/Common）ごとのモジュールを定義し、依存ライブラリ、JDK を設定。
  - `registerProjectServices()`: Analysis API で必要な各種サービスを登録（`KotlinDeclarationProviderFactory`, `KotlinPackageProviderFactory`, `KotlinDirectInheritorsProvider`, etc.）
  - `prepareAllKSFiles()`: ソースモジュールの `psiRoots` から `KSFileImpl` / `KSFileJavaImpl` を生成。
  - `prepareNewKSFiles()`: 生成されたファイルを `IncrementalGlobalSearchScope` や `IncrementalKotlinDeclarationProviderFactory` に追加してから KSFile 化。
- **動作の詳細**:
  1. `setupIdeaStandaloneExecution()` で headless モードに設定。
  2. `createAASession()` で Analysis API セッションを構築。
  3. `KSPCoreEnvironment`、`IncrementalJavaFileManager`（JVM のみ）を初期化。
  4. `ResolverAAImpl.ktModule` に単一の `KaSourceModule` をセット。
  5. `IncrementalContextAA.calcDirtyFiles()` でダーティファイルを算出。
  6. `SymbolProcessorEnvironment` を構築してプロセッサを生成。
  7. ラウンドループ: `ResolverAAImpl` を生成 → `processor.process()` → 生成ファイルを `prepareNewKSFiles()` で追加 → `dropCaches()` でキャッシュクリア → 生成ファイルなし or エラーで終了。
  8. エラー/完了に応じて `onError()` / `finish()` を呼ぶ。
  9. finally ブロックで `Disposer.dispose()` によりリソースを解放。

#### `ResolverAAImpl` (K2 / Resolver 実装)
- **ファイル**: `https://github.com/google/ksp/blob/main/kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/ResolverAAImpl.kt`
- **継承/実装**: `Resolver`
- **特記事項**:
  - `ThreadLocal` でインスタンス管理 (`instance`, `ktModule`)。
  - `KaFirSymbol` / `KaFirType` を通じて Analysis API の FIR シンボルにアクセス。
  - `tearDown()` でスレッドローカルをクリア。

#### `KspGradleSubplugin` (K2 / Gradle plugin)
- **ファイル**: `https://github.com/google/ksp/blob/main/gradle-plugin/src/main/kotlin/com/google/devtools/ksp/gradle/KspSubplugin.kt`
- **継承/実装**: `KotlinCompilerPluginSupportPlugin`
- **オーバーライドメソッド**:
  - `apply(target)`: `KspExtension` を作成。デフォルトで `useKsp2=true`。AGP 検出時にバリアントキャッシュを初期化。
  - `isApplicable(kotlinCompilation)`: KSP1 が選択されていた場合は例外をスロー（KSP1 廃止）。常に `true` を返す。
  - `applyToCompilation(kotlinCompilation)`: `KspAATask` を登録し、生成された Kotlin/Java ソースを `kotlinCompilation.defaultSourceSet` に追加。
  - `getCompilerPluginId()`: `"com.google.devtools.ksp.symbol-processing"`
  - `getPluginArtifact()`: `symbol-processing-api` アーティファクト
- **動作の詳細**: K2 モードでは `KspAATask` が KSP 処理の本体。`KotlinCompilerPluginSupportPlugin` のプラグインアーティファクト登録は形式的なもので、実際の処理は `KspAATask` が `KSPLoader.loadAndRunKSP()` を呼ぶことで実行される。生成ディレクトリは `build/generated/ksp/<target>/<sourceSet>/` 以下。

---

## atomicfu

### 登録エントリポイント
- Kotlin リポジトリ内のパス: `plugins/atomicfu/atomicfu-compiler/`
- サービスファイル: `resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar`
  - 登録クラス: `org.jetbrains.kotlinx.atomicfu.compiler.extensions.AtomicfuComponentRegistrar`

### 各機能の詳細

#### `AtomicfuComponentRegistrar` (K2 / CompilerPluginRegistrar)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/extensions/AtomicfuComponentRegistrar.kt`
- **継承/実装**: `CompilerPluginRegistrar`
- **オーバーライドメソッド**:
  - `registerExtensions(configuration)`: `registerExtensions(this)` に委譲
  - `pluginId`: `"org.jetbrains.kotlinx.atomicfu"`
  - `supportsK2`: `true`
- **登録内容**:
  - `FirExtensionRegistrar.registerExtension(AtomicfuFirExtensionRegistrar())` → FIR フェーズの checker を登録
  - `IrGenerationExtension.registerExtension(AtomicfuLoweringExtension())` → IR 変換を登録

#### `AtomicfuFirExtensionRegistrar` (FIR / K2)
- **継承/実装**: `FirExtensionRegistrar`
- **オーバーライドメソッド**:
  - `configurePlugin()`: `+::AtomicfuFirCheckers` と `registerDiagnosticContainers(AtomicfuErrors)` を登録
- **動作の詳細**: FIR フェーズで atomicfu に関する診断メッセージを有効にする。

#### `AtomicfuFirCheckers` (FIR / K2)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/extensions/AtomicfuFirCheckers.kt`
- **継承/実装**: `FirAdditionalCheckersExtension(session)`
- **オーバーライドメソッド**:
  - `declarationCheckers.propertyCheckers`: `AtomicfuPropertyChecker` を登録
  - `expressionCheckers.functionCallCheckers`: `AtomicfuAtomicRefToPrimitiveCallChecker` を登録
  - `typeCheckers.resolvedTypeRefCheckers`: 現在は空
- **動作の詳細**: atomic プロパティに関する宣言レベルおよび関数呼び出しレベルのチェックを FIR フェーズで実行する。

#### `AtomicfuPropertyChecker` (FIR / K2 / プロパティチェッカー)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/diagnostic/AtomicfuPropertyChecker.kt`
- **継承/実装**: `FirPropertyChecker(MppCheckerKind.Common)`
- **オーバーライドメソッド**:
  - `check(declaration: FirProperty)` (`context(CheckerContext, DiagnosticReporter)`)
- **動作の詳細**:
  1. `kotlinx.atomicfu` パッケージの型でない場合は無視。
  2. `public` または `@PublishedApi` なプロパティ（含むクラスも同様）に対して `PUBLIC_ATOMICS_ARE_FORBIDDEN` または `PUBLISHED_API_ATOMICS_ARE_FORBIDDEN` を報告。
  3. `var` として宣言されている場合は `ATOMIC_PROPERTIES_SHOULD_BE_VAL` を報告。
- **診断メッセージ**:
  - `PUBLIC_ATOMICS_ARE_FORBIDDEN`: public な atomic プロパティを private/internal に変更するよう促す。
  - `PUBLISHED_API_ATOMICS_ARE_FORBIDDEN`: `@PublishedApi` な atomic プロパティへの警告。
  - `ATOMIC_PROPERTIES_SHOULD_BE_VAL`: `var` で宣言された atomic プロパティへの警告。

#### `AtomicfuAtomicRefToPrimitiveCallChecker` (FIR / K2 / 関数呼び出しチェッカー)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/diagnostic/AtomicfuAtomicRefToPrimitiveCallChecker.kt`
- **継承/実装**: `AbstractAtomicReferenceToPrimitiveCallChecker(atomicByPrimitive, MppCheckerKind.Platform, ...)`
- **動作の詳細**: `AtomicRef<Boolean/Int/Long>` のような primitive 型での `compareAndSet` / `compareAndExchange` 呼び出しを検出し、専用の型（`AtomicBoolean` など）の使用を促す。

#### `AtomicfuLoweringExtension` (IR / K2)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/extensions/AtomicfuLoweringExtension.kt`
- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**:
  - `generate(moduleFragment, pluginContext)`: プラットフォームに応じてトランスフォーマーを選択
- **動作の詳細**:
  - JVM: `AtomicfuJvmIrTransformer(JvmAtomicSymbols(pluginContext, moduleFragment), pluginContext).transform(moduleFragment)`
  - Native: `AtomicfuNativeIrTransformer(NativeAtomicSymbols(pluginContext, moduleFragment), pluginContext).transform(moduleFragment)`
  - JS: ファイルごとに `AtomicfuClassLowering.runOnFileInOrder(file)` → `AtomicfuJsIrTransformer.transform(irFile)` を呼ぶ。JS は `runOnFilePostfix` の代わりに親 → 子の順（`runOnFileInOrder`）で処理。

#### `AbstractAtomicfuTransformer` (IR 変換の基底クラス)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/backend/common/AbstractAtomicfuTransformer.kt`
- **継承/実装**: なし（abstract）
- **サブクラス**: `AtomicfuJvmIrTransformer`, `AtomicfuNativeIrTransformer`, `AtomicfuJsIrTransformer`
- **抽象プロパティ**:
  - `atomicfuSymbols: AbstractAtomicSymbols`
  - `atomicfuExtensionsTransformer: AtomicExtensionTransformer`
  - `atomicfuPropertyTransformer: AtomicPropertiesTransformer`
  - `atomicfuFunctionCallTransformer: AtomicFunctionCallTransformer`
- **主要メソッド**:
  - `transform(moduleFragment)`: 以下の順序で変換を実行:
    1. `transformAtomicProperties` — atomic プロパティを volatile プロパティ／atomic field updater に変換
    2. `transformAtomicExtensions` — atomic 拡張関数を変換
    3. `transformAtomicFunctions` — atomic 操作の関数呼び出しを変換
    4. `remapValueParameters` — 値パラメータを再マップ
    5. `finalTransformationCheck` — 変換後の検証
    6. `patchDeclarationParents` — 親宣言の修正
- **重要な内部マップ**:
  - `atomicfuPropertyToVolatile`: atomic プロパティ → 対応する volatile プロパティ
  - `atomicfuPropertyToAtomicHandler`: atomic プロパティ → `AtomicHandler<*>` (field updater / atomic array)
- **定数 (companion object)**:
  - `VOLATILE = "\$volatile"`, `AFU_PKG = "kotlinx.atomicfu"`, `ATOMIC_TYPES`, `ATOMIC_ARRAY_TYPES`, `ATOMICFU_LOOP_FUNCTIONS` など

#### `AtomicfuJvmIrTransformer` (IR / JVM 専用)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/backend/jvm/AtomicfuJvmIrTransformer.kt`
- **継承/実装**: `AbstractAtomicfuTransformer`
- **動作の詳細**:
  - 拡張関数を 3 種類のハンドラ用に複製: `ATOMIC_FIELD_UPDATER`, `BOXED_ATOMIC`, `ATOMIC_ARRAY`
  - デリゲートプロパティ: 対応する volatile プロパティが存在すれば volatile アクセサに委譲、なければ `BoxedAtomic` アクセサに委譲

#### `AtomicfuNativeIrTransformer` (IR / Native 専用)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/backend/native/AtomicfuNativeIrTransformer.kt`
- **継承/実装**: `AbstractAtomicfuTransformer`
- **動作の詳細**:
  - 拡張関数を 2 種類のハンドラ用に複製: `NATIVE_PROPERTY_REF`, `ATOMIC_ARRAY`
  - デリゲートプロパティは必ず対応する volatile プロパティが存在することを前提（`requireNotNull`）

#### `AtomicfuKotlinGradleSubplugin` (Gradle plugin)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/libraries/tools/atomicfu/src/common/kotlin/org/jetbrains/kotlinx/atomicfu/gradle/AtomicfuKotlinGradleSubplugin.kt`
- **継承/実装**: `KotlinCompilerPluginSupportPlugin`
- **オーバーライドメソッド**:
  - `apply(target)`: `atomicfuCompilerPlugin` Extension を作成
  - `isApplicable(kotlinCompilation)`: `AtomicfuKotlinGradleExtension` の各プラットフォームフラグが有効かつ対応プラットフォームかどうかを確認
  - `applyToCompilation()`: 空のオプションリストを返す（オプション不要）
  - `getPluginArtifact()`: `kotlin-atomicfu-compiler-plugin-embeddable`
  - `getCompilerPluginId()`: `"org.jetbrains.kotlinx.atomicfu"`
- **動作の詳細**: `isJsIrTransformationEnabled`, `isJvmIrTransformationEnabled`, `isNativeIrTransformationEnabled` の 3 フラグで適用対象プラットフォームを絞る。デフォルトはいずれも `false`。

#### `AtomicfuErrors` (診断エラー定義)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/diagnostic/AtomicfuErrors.kt`
- **継承/実装**: `KtDiagnosticsContainer`
- **定義されているエラー**:
  - `PUBLIC_ATOMICS_ARE_FORBIDDEN`: `error1<KtProperty, String>` / `VISIBILITY_MODIFIER` 位置
  - `PUBLISHED_API_ATOMICS_ARE_FORBIDDEN`: `error1<KtProperty, String>` / `VISIBILITY_MODIFIER` 位置
  - `ATOMIC_PROPERTIES_SHOULD_BE_VAL`: `error1<KtProperty, String>` / `VAL_OR_VAR_NODE` 位置
- **レンダラー**: `AtomicfuErrorMessages` が各エラーのメッセージテキストを定義（使用例付きの詳細な説明）

#### `AtomicfuStandardClassIds` (クラス ID 定義)
- **ファイル**: `https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/backend/AtomicfuStandardClassIds.kt`
- **定義内容**:
  - `BASE_ATOMICFU_PACKAGE = FqName("kotlinx.atomicfu")`
  - `AtomicBoolean`, `AtomicInt`, `AtomicLong`, `AtomicRef`, `AtomicBooleanArray`, `AtomicIntArray`, `AtomicLongArray`, `AtomicArray` の `ClassId`
  - `atomicByPrimitive`: `Boolean/Int/Long` → 対応する Atomic 型へのマップ（`AtomicRefToPrimitiveCallChecker` で使用）
  - `Callables`: `atomic`, `atomicRefCompareAndSet`, `atomicRefCompareAndExchange` の `CallableId`
