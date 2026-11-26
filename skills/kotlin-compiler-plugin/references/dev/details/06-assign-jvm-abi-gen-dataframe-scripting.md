# assign-plugin / jvm-abi-gen / kotlin-dataframe / scripting 詳細

## assign-plugin

### 登録エントリポイント

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/assign-plugin/assign-plugin.cli/src/org/jetbrains/kotlin/assignment/plugin/ValueContainerAssignmentPlugin.kt
- 登録内容:
  - `AssignmentCommandLineProcessor`: CLI オプション `annotation` を受け取り `ASSIGNMENT_ANNOTATION` リストに追加
  - `AssignmentComponentRegistrar` (`CompilerPluginRegistrar`): `supportsK2 = true`
    - K1: `AssignResolutionAltererExtension` に `CliAssignPluginResolutionAltererExtension` を登録
    - K1: `StorageComponentContainerContributor` に `AssignmentPluginDeclarationChecker` を登録
    - K2: `FirExtensionRegistrar` に `FirAssignmentPluginExtensionRegistrar` を登録

### 各機能の詳細

#### FirAssignmentPluginExtensionRegistrar (FIR / K2)

- **継承/実装**: `FirExtensionRegistrar`
- **登録内容**:
  - `FirAssignmentPluginAssignAltererExtension`
  - `FirAssignmentPluginCheckersExtension`
  - `FirAssignAnnotationMatchingService.getFactory(annotations)` (セッションコンポーネント)
  - `FirErrorsAssignmentPlugin` (診断コンテナ)

#### FirAssignmentPluginAssignAltererExtension (FIR / K2)

- **継承/実装**: `FirAssignExpressionAltererExtension(session)`
- **オーバーライドメソッド**: `transformVariableAssignment(variableAssignment: FirVariableAssignment): FirStatement?`
- **動作の詳細**:
  - 左辺が `val` プロパティであり、その型が `FirAssignAnnotationMatchingService` でアノテーション付きと判定される場合のみ変換する
  - `a = b` という代入式を `a.assign(b)` という `FirFunctionCall` に書き換える
  - `source` に `KtFakeSourceElementKind.AssignmentPluginAltered` を付与して後続チェッカーが識別できるようにする

#### FirAssignAnnotationMatchingService (FIR SessionComponent / K2)

- **継承/実装**: `FirExtensionSessionComponent(session)`
- **動作の詳細**:
  - `FirCache<FirRegularClassSymbol, Boolean>` でマッチング結果をキャッシュ
  - クラスとすべてのスーパータイプを再帰的にたどり、指定アノテーション FQN リストのどれかが付与されているか確認する
  - `FirSession.annotationMatchingService` 拡張プロパティで任意の場所からアクセス可能

#### FirAssignmentPluginCheckersExtension (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension(session)`
- **オーバーライドメソッド**:
  - `declarationCheckers.simpleFunctionCheckers` → `FirAssignmentPluginFunctionChecker`
  - `expressionCheckers.functionCallCheckers` → `FirAssignmentPluginFunctionCallChecker`

#### FirAssignmentPluginFunctionChecker (FIR Declaration Checker / K2)

- **継承/実装**: `FirSimpleFunctionChecker(MppCheckerKind.Common)`
- **オーバーライドメソッド**: `check(declaration: FirNamedFunction)`
- **動作の詳細**: `assign(value)` という名前・引数1個の関数を検出し、レシーバークラスがアノテーション付きなのに戻り値型が `Unit` でない場合にエラーを報告する
- **診断メッセージ**: `DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT`

#### FirAssignmentPluginFunctionCallChecker (FIR Expression Checker / K2)

- **継承/実装**: `FirFunctionCallChecker(MppCheckerKind.Common)`
- **オーバーライドメソッド**: `check(expression: FirFunctionCall)`
- **動作の詳細**:
  - `source.kind == KtFakeSourceElementKind.AssignmentPluginAltered` かつ引数1個のコールのみを対象とする
  - 解決失敗 → `NO_APPLICABLE_ASSIGN_METHOD`
  - 解決成功だが戻り値が `Unit` でない → `CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT`
- **診断メッセージ**:
  - `NO_APPLICABLE_ASSIGN_METHOD` (`error0<KtElement>(OPERATOR)`)
  - `CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT` (`error0<KtElement>(OPERATOR)`)

#### AbstractAssignPluginResolutionAltererExtension (K1)

- **継承/実装**: `AssignResolutionAltererExtension` (内部不安定 API)
- **オーバーライドメソッド**:
  - `needOverloadAssign`: 左辺が `val` プロパティかつ型がアノテーション付きなら `true`
  - `resolveAssign`: 一時トレースで `assign()` を `CallResolver` で解決し、成功・Unit 戻り値なら確定、失敗なら診断を報告する
- **診断メッセージ**: `CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT`, `NO_APPLICABLE_ASSIGN_METHOD`

---

## jvm-abi-gen

### 登録エントリポイント

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/jvm-abi-gen/src/org/jetbrains/kotlin/jvm/abi/JvmAbiComponentRegistrar.kt
- 登録内容:
  - `JvmAbiComponentRegistrar` (`CompilerPluginRegistrar`): `supportsK2 = true`
    - `ClassGeneratorExtension` に `JvmAbiClassBuilderInterceptor` を登録 (IR パス)
    - `ClassFileFactoryFinalizerExtension` に `JvmAbiOutputExtension` を登録 (最終出力パス)

### CLI オプション (JvmAbiCommandLineProcessor)

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/jvm-abi-gen/src/org/jetbrains/kotlin/jvm/abi/JvmAbiCommandLineProcessor.kt
- オプション一覧:
  - `outputDir` (必須): 出力先パス (.jar またはディレクトリ)
  - `removeDebugInfo` (デフォルト false): デバッグ情報削除
  - `removeDataClassCopyIfConstructorIsPrivate` (デフォルト false): private コンストラクタのデータクラスから `copy` 関数を除去
  - `preserveDeclarationOrder` (デフォルト false): メンバーをソートせず元の順序を維持
  - `removePrivateClasses` (デフォルト false): private クラスを ABI から除去
  - `treatInternalAsPrivate` (デフォルト false): internal 宣言を private 扱いで除去

### 各機能の詳細

#### JvmAbiClassBuilderInterceptor (IR Extension)

- **継承/実装**: `ClassGeneratorExtension`
- **オーバーライドメソッド**: `generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator`
- **動作の詳細**:
  - 各クラスに対して `AbiInfoClassGenerator` ラッパーを返し、生成されるフィールド・メソッドの ABI 情報を収集する
  - `AbiClassInfo` として3種類:
    - `AbiClassInfo.Public`: アノテーションクラス・`METADATA_PUBLIC_ABI_FLAG` 付きクラスをそのまま保持
    - `AbiClassInfo.Stripped(memberInfo)`: public/internal メンバーを保持しつつ非 inline メソッドボディをストリップ
    - `AbiClassInfo.Deleted`: private クラス・local/anonymous クラス・`$WhenMappings` クラスを削除
  - フィールド: private → 除去、それ以外 → `AbiMethodInfo.KEEP`
  - メソッド: `<clinit>`・`access$` 合成アクセサ・private → 除去、`$$forInline` 付き inline suspend → `KEEP`、inline → `KEEP`、それ以外 → `STRIP`

#### JvmAbiOutputExtension (ClassFileFactory Finalizer Extension)

- **継承/実装**: `ClassFileFactoryFinalizerExtension`
- **オーバーライドメソッド**: `finalizeClassFactory(factory: ClassFileFactory)`
- **動作の詳細**:
  - `abiClassInfoBuilder()` で全クラスの ABI 情報を取得し、ASM で各クラスファイルを加工して出力する
  - `Deleted` → スキップ、`Public` → バイトコードそのままコピー
  - `Stripped` → ASM `ClassVisitor` で:
    - `visitField`: `KEEP` でないフィールドを除去
    - `visitMethod`: `KEEP` → そのままコピー、`STRIP` → `BodyStrippingMethodVisitor` でボディを `ACONST_NULL; ATHROW` に置換
    - `visitInnerClass`: 実際に使用されている型のみ `InnerClasses` 属性に残す
    - `@Metadata` アノテーション: `JvmAbiMetadataProcessor` で private 宣言を除去
  - `preserveDeclarationOrder = false` の場合、フィールド・メソッドをアルファベット順にソートして出力安定性を確保

#### BodyStrippingMethodVisitor

- **動作**: `visitCode()` 時に `ACONST_NULL; ATHROW` のみを emit して return し、以降の命令への委譲を停止する

---

## kotlin-dataframe

### 登録エントリポイント

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.cli/src/org/jetbrains/kotlinx/dataframe/plugin/FirDataFrameComponentRegistrar.kt
- 登録内容:
  - `FirDataFrameComponentRegistrar` (`CompilerPluginRegistrar`): `supportsK2 = true`
    - `FirExtensionRegistrar` に `FirDataFrameExtensionRegistrar` を登録
    - `IrGenerationExtension` に `IrBodyFiller` を登録

### FirDataFrameExtensionRegistrar

- 登録内容:
  - `TopLevelExtensionsGenerator` (`FirDeclarationGenerationExtension`)
  - `ReturnTypeBasedReceiverInjector`
  - `FunctionCallTransformer` (`FirFunctionCallRefinementExtension`)
  - `TokenContentGenerator` (`FirDeclarationGenerationExtension`)
  - `DataRowSchemaSupertype`
  - `ExpressionAnalysisAdditionalChecker`
  - contextReader が非 null の場合: `ImportedSchemasGenerator`, `ImportedSchemasCompanionGenerator`, `ImportedSchemasCheckers`, `ImportedSchemasService`
  - 診断コンテナ: `FirDataFrameErrors`, `ImportedSchemasDiagnostics`, `SchemaInfoDiagnostics`

### 各機能の詳細

#### FunctionCallTransformer (FIR / K2)

- **継承/実装**: `FirFunctionCallRefinementExtension(session)`, `KotlinTypeFacade`
- **オーバーライドメソッド**: `intercept(callInfo, symbol)`, `transform(call, originalSymbol)`
- **動作の詳細**:
  - `@Refine` アノテーション付き関数呼び出しに反応する。`@DisableInterpretation` 付きコール・inline 関数内・プロパティアクセサ内は除外する
  - 対応する内部 `CallTransformer` が4種類: `DataFrameCallTransformer`、`DataRowCallTransformer`、`ColumnGroupCallTransformer`、`GroupByCallTransformer`
  - `intercept` フェーズ: 新しい型引数となるローカルクラス (`DataFrameType_XXXX`) のシンボルを生成して `CallReturnType` として返す
  - `transform` フェーズ: `analyzeRefinedCallShape` でスキーマを解析し `materialize` で FIR クラスを構築。元の呼び出しを `let { localClasses; originalCall(it) }` の形に書き換える
  - スキーマ具体化: `SimpleDataColumn` → メンバープロパティ、`SimpleColumnGroup` → `ColumnGroup<T>` 型プロパティ、`SimpleFrameColumn` → `DataFrame<T>` 型プロパティ

#### TokenContentGenerator (FIR Declaration Generation / K2)

- **継承/実装**: `FirDeclarationGenerationExtension(session)`
- **オーバーライドメソッド**: `getCallableNamesForClass`, `generateProperties`, `generateConstructors`
- **動作の詳細**:
  - `FunctionCallTransformer` が生成したローカルクラス (`callShapeData` 設定済み) にプロパティとコンストラクタを生成する
  - `CallShapeData.Schema` → `@Order` アノテーション付きメンバープロパティを生成
  - `CallShapeData.RefinedType` → スコープクラスへの参照プロパティを生成
  - `CallShapeData.Scope` → `DataRow<T>` レシーバーと `ColumnsScope<T>` レシーバーそれぞれの拡張プロパティを生成

#### TopLevelExtensionsGenerator (FIR Declaration Generation / K2)

- **継承/実装**: `FirDeclarationGenerationExtension(session)`
- **オーバーライドメソッド**: `getTopLevelCallableIds`, `generateProperties`, `FirDeclarationPredicateRegistrar.registerPredicates`
- **動作の詳細**:
  - `@DataSchema` アノテーション付きトップレベルクラスに対して `DataRow<T>` と `ColumnsContainer<T>` への拡張プロパティをトップレベルに生成する
  - `LookupPredicate` で対象クラスを事前フィルタリングする
  - 戻り値型変換: `@DataSchema` 型 → `DataRow<T>`、`List<@DataSchema T>` → `DataFrame<T>`、それ以外 → `DataColumn<T>`

#### IrBodyFiller (IR Extension)

- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**:
  - `DataFrameFileLowering` を実行:
    - 生成コンストラクタにボディ (委譲コンストラクタ呼び出し + インスタンス初期化子) を追加
    - 生成プロパティの getter に `@JvmName` アノテーションと `DataRow[colName]`/`ColumnsScope[colName]` の型キャスト付き呼び出しをボディとして設定
    - Scope クラスを受け取る呼び出しでは `Scope()` コンストラクタ呼び出しを引数として注入
  - `IrImportedSchemaGenerator` を実行: `@DataSchemaSource` 付きクラスへのスキーマプロパティ生成

---

## scripting

### 登録エントリポイント

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/pluginRegisrar.kt
- 登録内容:
  - `ScriptingK2CompilerPluginRegistrar` (`CompilerPluginRegistrar`): `supportsK2 = true`
    - `FirExtensionRegistrar` に `FirScriptingCompilerExtensionRegistrar` を登録
    - `FirExtensionRegistrar` に `FirScriptingSamWithReceiverExtensionRegistrar` を登録
    - `CollectAdditionalSourceFilesExtension` に `CollectAdditionalScriptSourcesExtension` を登録
  - `ScriptingCompilerConfigurationComponentRegistrar` (旧 `ComponentRegistrar`、K1 互換のため残存):
    - `CompilerConfigurationExtension`, `CollectAdditionalSourcesExtension`, `ProcessSourcesBeforeCompilingExtension`
    - `ScriptEvaluationExtension` (`JvmCliScriptEvaluationExtension`)
    - `IrGenerationExtension` (`ScriptLoweringExtension`, `ReplLoweringExtension`)

### FirScriptingCompilerExtensionRegistrar

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/FirScriptingCompilerExtensionRegistrar.kt
- 登録内容:
  - `FirScriptDefinitionProviderService.getFactory(compilerConfiguration)` (セッションコンポーネント)
  - `FirScriptConfiguratorExtensionImpl.getFactory()`
  - `FirScriptResolutionConfigurationExtensionImpl.getFactory()`
  - `Fir2IrScriptConfiguratorExtensionImpl.getFactory()`
  - `DISABLE_SCRIPTING_PLUGIN_OPTION = true` の場合は何も登録しない

### 各機能の詳細

#### FirScriptConfiguratorExtensionImpl (FIR / K2)

- **継承/実装**: `FirScriptConfiguratorExtension(session)`
- **オーバーライドメソッド**: `accepts(sourceFile, scriptSource)`, `configure(sourceFile, context)` (FirScriptBuilder 上)
- **動作の詳細**:
  - `getOrLoadConfiguration()` でスクリプトのコンパイル設定 (`ScriptCompilationConfiguration`) を取得する
  - `baseClass` → `FirScriptReceiverParameter` を追加し、ベースクラスのコンストラクタパラメータを `parameters` に追加
  - `implicitReceivers` → `FirScriptReceiverParameter` を複数追加
  - `providedProperties` → ローカル `FirProperty` を `parameters` に追加
  - `resultField` → スクリプトの最後の式を `FirProperty` に変換して `declarations` に追加
  - `annotationsForSamWithReceivers` → `knownAnnotationsForSamWithReceiver` セットに追加
  - 設定取得失敗時は `FirAnonymousInitializer` でエラーブロックを先頭に挿入する

#### ScriptsToClassesLowering (IR Lowering / K2)

- **継承/実装**: `ModuleLoweringPass`
- **オーバーライドメソッド**: `lower(irModule: IrModuleFragment)`
- **動作の詳細**:
  - モジュール内のすべての `IrScript` ノードを `IrClass` に変換する
  - `prepareScriptClass`: `IrDeclarationOrigin.SCRIPT_CLASS` を持つ `IrClass` を作成
  - `finalizeScriptClass`: スクリプトの statements を処理:
    - `IrVariable` → クラスのバッキングフィールド付きプロパティに変換
    - `IrDeclaration` → クラスのメンバーとしてそのまま移動
    - その他の式 → `IrAnonymousInitializer` (インスタンス初期化子) として包む
  - implicit receiver は `$$implicitReceiver_TypeName` という名前の private フィールドに変換
  - earlier scripts は `$$earlierScripts` フィールドに格納
  - `main()` 関数がない場合は自動生成し `RunnerKt.runCompiledScript(ScriptClass::class.java, args)` を呼び出す
  - スクリプト間の依存関係は `topologicalSort` で処理する

#### AbstractScriptEvaluationExtension (CLI Extension)

- **継承/実装**: `ScriptEvaluationExtension` (抽象クラス)
- **主要メソッド**: `eval(arguments, configuration, projectEnvironment)`
- **動作の詳細**:
  - スクリプトモード (`-script`) または式評価モード (`-expression`) を区別して `SourceCode` を構築する
  - `ScriptDefinitionProvider` でスクリプト定義を取得し、コンパイル設定と評価設定を確定する
  - `createScriptCompiler()` / `createScriptEvaluator()` (サブクラスが実装) でコンパイルと評価を実行する
  - `ResultValue.Value` → stdout に出力して `OK`、`ResultValue.Error` → stderr に出力して `SCRIPT_EXECUTION_ERROR`
  - 具体実装: `JvmCliScriptEvaluationExtension`
