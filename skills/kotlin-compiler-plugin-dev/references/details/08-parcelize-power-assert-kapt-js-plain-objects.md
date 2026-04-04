# parcelize / power-assert / kapt / js-plain-objects 詳細

## parcelize

### 登録エントリポイント
- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.cli/src/org/jetbrains/kotlin/parcelize/ParcelizeComponentRegistrar.kt
- 登録内容:
  - `IrGenerationExtension` → `ParcelizeFirIrGeneratorExtension` (K2用) / `ParcelizeIrGeneratorExtension` (K1用)
  - `SyntheticResolveExtension` → `ParcelizeResolveExtension` (K1用)
  - `StorageComponentContainerContributor` → `ParcelizeDeclarationCheckerComponentContainerContributor` (K1用)
  - `FirExtensionRegistrar` → `FirParcelizeExtensionRegistrar` (K2用)

### 機能一覧

| クラス名 | FIR/IR | K2/K1 | ソースURL |
|---|---|---|---|
| FirParcelizeExtensionRegistrar | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/FirParcelizeExtensionRegistrar.kt) |
| FirParcelizeDeclarationGenerator | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/FirParcelizeDeclarationGenerator.kt) |
| FirParcelizeCheckersExtension | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/FirParcelizeCheckersExtension.kt) |
| FirParcelizeClassChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/diagnostics/FirParcelizeClassChecker.kt) |
| FirParcelizeAnnotationChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/diagnostics/FirParcelizeAnnotationChecker.kt) |
| FirParcelizePropertyChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/diagnostics/FirParcelizePropertyChecker.kt) |
| FirParcelizeFunctionChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/diagnostics/FirParcelizeFunctionChecker.kt) |
| FirParcelizeConstructorChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/diagnostics/FirParcelizeConstructorChecker.kt) |
| ParcelizeFirIrGeneratorExtension | IR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.backend/src/org/jetbrains/kotlin/parcelize/ParcelizeFirIrGeneratorExtension.kt) |
| ParcelizeFirIrTransformer | IR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.backend/src/org/jetbrains/kotlin/parcelize/ParcelizeFirIrTransformer.kt) |
| ParcelizeIrGeneratorExtension | IR | K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.backend/src/org/jetbrains/kotlin/parcelize/ParcelizeIrGeneratorExtension.kt) |
| ParcelizeIrTransformerBase | IR | 共通 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.backend/src/org/jetbrains/kotlin/parcelize/ParcelizeIrTransformerBase.kt) |
| ParcelizeResolveExtension | - | K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k1/src/org/jetbrains/kotlin/parcelize/ParcelizeResolveExtension.kt) |
| ParcelizeDeclarationChecker | - | K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k1/src/org/jetbrains/kotlin/parcelize/ParcelizeDeclarationChecker.kt) |
| KtErrorsParcelize (診断定義) | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/diagnostics/KtErrorsParcelize.kt) |

### 各機能の詳細

#### FirParcelizeDeclarationGenerator (FIR / K2)
- **継承/実装**: `FirDeclarationGenerationExtension`
- **オーバーライドメソッド**:
  - `generateFunctions(callableId, context)` - メンバ関数を生成
  - `getCallableNamesForClass(classSymbol, context)` - 生成する関数名セットを返す
  - `FirDeclarationPredicateRegistrar.registerPredicates()` - predicate を登録
- **動作の詳細**:
  - `@Parcelize` アノテーション付きクラスに対して `describeContents()` と `writeToParcel(Parcel, Int)` メソッドを FIR レベルで自動生成する
  - `describeContents()` は、クラス自身またはスーパークラスに既存の実装がない場合のみ生成
  - `writeToParcel()` は、同シグネチャの関数が未宣言の場合のみ生成
  - abstract/sealed クラスには生成しない（`getCallableNamesForClass` で空セットを返す）
  - `ParcelizePluginKey` を `GeneratedDeclarationKey` として使用

#### FirParcelizeCheckersExtension (FIR / K2)
- **継承/実装**: `FirAdditionalCheckersExtension`
- **登録チェッカー**:
  - `expressionCheckers.annotationCallCheckers` → `FirParcelizeAnnotationChecker`
  - `declarationCheckers.classCheckers` → `FirParcelizeClassChecker`
  - `declarationCheckers.propertyCheckers` → `FirParcelizePropertyChecker`
  - `declarationCheckers.simpleFunctionCheckers` → `FirParcelizeFunctionChecker`
  - `declarationCheckers.constructorCheckers` → `FirParcelizeConstructorChecker`

#### FirParcelizeClassChecker (FIR / K2)
- **継承/実装**: `FirClassChecker`
- **チェック内容**:
  - `@Parcelize` クラスが通常のクラスであること（interface/annotation/enum不可）
  - companion object の名前が "CREATOR" でないこと
  - inner class でないこと
  - local class でないこと
  - abstract でないこと
  - `Parcelable` をスーパータイプに持つこと
  - primary constructor が存在すること
  - primary constructor が空でないこと
  - 非推奨の `Parceler` インターフェースの使用を検出

#### FirParcelizeAnnotationChecker (FIR / K2)
- **継承/実装**: `FirAnnotationCallChecker`
- **チェック内容**:
  - `@TypeParceler` の重複検出
  - `@TypeParceler` が `@Parcelize` クラス内でのみ使用されていること
  - `@WriteWith` のパラメータが object 型であること
  - `@WriteWith` の対象型とパーセラーの型パラメータの一致
  - 非推奨アノテーション使用の検出

#### ParcelizeFirIrTransformer (IR / K2)
- **継承/実装**: `ParcelizeIrTransformerBase` (→ `IrVisitorVoid`)
- **主要メソッド**:
  - `transform(moduleFragment)` - モジュール全体を変換
  - `visitClass(declaration)` - クラスごとの処理
- **動作の詳細**:
  - FIR の `ParcelizePluginKey` で生成された関数にボディを追加
  - `describeContents()` にファイルディスクリプタフラグを返すボディを生成
  - `writeToParcel()` に各プロパティをパーセルに書き込むボディを生成
  - `CREATOR` コンパニオンオブジェクト（`Parcelable.Creator` 実装）を生成
  - 継承コンストラクタのサポート（experimental code generation モード）
  - `parcelableCreatorOf<T>()` 組み込み関数の IR 変換

#### ParcelizeResolveExtension (K1)
- **継承/実装**: `SyntheticResolveExtension`
- **オーバーライドメソッド**:
  - `getSyntheticFunctionNames(thisDescriptor)` - 合成関数名を返す
  - `generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)` - 合成メソッドを生成
- **動作の詳細**:
  - K1 コンパイラ用に `describeContents()` と `writeToParcel()` を記述子レベルで合成
  - sealed クラスには生成しない

#### ParcelizeDeclarationChecker (K1)
- **継承/実装**: `DeclarationChecker`
- **オーバーライドメソッド**: `check(declaration, descriptor, context)`
- **動作の詳細**: FirParcelizeClassChecker と同等のチェック（K1用実装）

#### 診断メッセージ (KtErrorsParcelize)
計26個の診断定義:
- `PARCELABLE_SHOULD_BE_CLASS`, `PARCELABLE_SHOULD_BE_INSTANTIABLE`
- `PARCELABLE_CANT_BE_INNER_CLASS`, `PARCELABLE_CANT_BE_LOCAL_CLASS`
- `NO_PARCELABLE_SUPERTYPE`, `PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR`
- `PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY`
- `PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR`
- `PROPERTY_WONT_BE_SERIALIZED`, `OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED`
- `CREATOR_DEFINITION_IS_NOT_ALLOWED`, `PARCELABLE_TYPE_NOT_SUPPORTED`
- `PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED`, `PARCELABLE_DELEGATE_IS_NOT_ALLOWED`
- `PARCELER_SHOULD_BE_OBJECT`, `PARCELER_TYPE_INCOMPATIBLE`
- `DUPLICATING_TYPE_PARCELERS`, `REDUNDANT_TYPE_PARCELER`
- `DEPRECATED_PARCELER`, `DEPRECATED_ANNOTATION`
- `CLASS_SHOULD_BE_PARCELIZE`, `VALUE_PARAMETER_USED_IN_CLASS_BODY`
- その他

---

## power-assert

### 登録エントリポイント
- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.cli/src/org/jetbrains/kotlin/powerassert/PowerAssertCompilerPluginRegistrar.kt
- 登録内容:
  - `IrGenerationExtension` → `PowerAssertIrGenerationExtension`
  - `FirExtensionRegistrar` → `PowerAssertFirExtensionRegistrar`
- 設定: `KEY_FUNCTIONS` で変換対象の完全修飾関数名リストを受け取る

### 機能一覧

| クラス名 | FIR/IR | K2/K1 | ソースURL |
|---|---|---|---|
| PowerAssertFirExtensionRegistrar | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.frontend/src/org/jetbrains/kotlin/powerassert/PowerAssertFirExtensionRegistrar.kt) |
| PowerAssertCheckersExtension | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.frontend/src/org/jetbrains/kotlin/powerassert/PowerAssertCheckersExtension.kt) |
| PowerAssertAnnotationChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.frontend/src/org/jetbrains/kotlin/powerassert/checkers/PowerAssertAnnotationChecker.kt) |
| PowerAssertRuntimeChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.frontend/src/org/jetbrains/kotlin/powerassert/checkers/PowerAssertRuntimeChecker.kt) |
| PowerAssertExplanationAccessChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.frontend/src/org/jetbrains/kotlin/powerassert/checkers/PowerAssertExplanationAccessChecker.kt) |
| PowerAssertIrGenerationExtension | IR | K2/K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.backend/src/org/jetbrains/kotlin/powerassert/PowerAssertIrGenerationExtension.kt) |
| PowerAssertCallTransformer | IR | K2/K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.backend/src/org/jetbrains/kotlin/powerassert/PowerAssertCallTransformer.kt) |
| PowerAssertFunctionTransformer | IR | K2/K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.backend/src/org/jetbrains/kotlin/powerassert/function/PowerAssertFunctionTransformer.kt) |
| PowerAssertFunctionFactory | IR | K2/K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.backend/src/org/jetbrains/kotlin/powerassert/function/PowerAssertFunctionFactory.kt) |
| PowerAssertDiagnostics (診断定義) | 共通 | K2/K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.common/src/org/jetbrains/kotlin/powerassert/PowerAssertDiagnostics.kt) |

### 各機能の詳細

#### PowerAssertFirExtensionRegistrar (FIR / K2)
- **継承/実装**: `FirExtensionRegistrar`
- **オーバーライドメソッド**: `configurePlugin()`
- **動作の詳細**:
  - `PowerAssertCheckersExtension` を登録
  - `PowerAssertDiagnostics` 診断コンテナを登録
  - FIR レベルでは宣言生成は行わない（チェッカーのみ）

#### PowerAssertCheckersExtension (FIR / K2)
- **継承/実装**: `FirAdditionalCheckersExtension`
- **登録チェッカー**:
  - `expressionCheckers.annotationCheckers` → `PowerAssertAnnotationChecker`
  - `expressionCheckers.functionCallCheckers` → `PowerAssertRuntimeChecker`
  - `expressionCheckers.propertyAccessExpressionCheckers` → `PowerAssertExplanationAccessChecker`

#### PowerAssertAnnotationChecker (FIR / K2)
- **継承/実装**: `FirAnnotationChecker`
- **チェック内容**:
  - `@PowerAssert` アノテーションが有効な関数にのみ適用されていることを検証
  - override 関数への `@PowerAssert` → `POWER_ASSERT_ILLEGAL_OVERRIDE` エラー
  - actual 関数への `@PowerAssert` → `POWER_ASSERT_ILLEGAL_ACTUAL` エラー

#### PowerAssertRuntimeChecker (FIR / K2)
- **継承/実装**: `FirFunctionCallChecker`
- **チェック内容**:
  - `@PowerAssert` 付き関数を呼び出す際にランタイムライブラリが利用可能かチェック
  - 利用不可の場合 `POWER_ASSERT_RUNTIME_UNAVAILABLE` 警告

#### PowerAssertExplanationAccessChecker (FIR / K2)
- **継承/実装**: `FirPropertyAccessExpressionChecker`
- **チェック内容**:
  - `PowerAssert.explanation` プロパティへのアクセスが `@PowerAssert` 関数内からのみであること
  - 違反時 `POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS` エラー

#### PowerAssertIrGenerationExtension (IR / K2+K1)
- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**:
  1. `PowerAssertBuiltIns` からランタイムシンボルをロード
  2. `PowerAssertFunctionTransformer` で `@PowerAssert` 付き関数を変換（`$powerassert` オーバーロード生成）
  3. `PowerAssertCallTransformer` で各ファイルの関数呼び出しを変換

#### PowerAssertCallTransformer (IR / K2+K1)
- **継承/実装**: `IrElementTransformerVoidWithContext`
- **オーバーライドメソッド**: `visitCall(expression)`
- **動作の詳細**:
  - **`@PowerAssert` アノテーション付き関数の呼び出し**: `CallExplanation` ベースの変換。`$powerassert` オーバーロードへのディスパッチ。式ツリーの各部分を変数に分解し、説明ラムダを生成
  - **設定ベースの関数（assert, require, check 等）の呼び出し**: ソースコードのダイアグラム文字列を生成し、メッセージパラメータ付きのオーバーロードへ変換
  - 再帰呼び出しと super 呼び出しは変換対象外
  - 定数式のみの場合 `POWER_ASSERT_CONSTANT` 情報診断を出力
  - 適切なオーバーロードが見つからない場合 `POWER_ASSERT_CAPABLE_OVERLOAD_MISSING` 警告

#### PowerAssertFunctionTransformer (IR / K2+K1)
- **継承/実装**: `DeclarationTransformer`
- **動作の詳細**:
  - `@PowerAssert` アノテーション付き関数を検出し、`PowerAssertFunctionFactory.generate()` で `$powerassert` 接尾辞付きの合成オーバーロードを生成
  - 元の関数内の `PowerAssert.explanation` アクセスを `null` に置換

#### PowerAssertFunctionFactory (IR / K2+K1)
- **動作の詳細**:
  - `@PowerAssert` 関数の `$powerassert` オーバーロードを生成
  - 元の関数を deepCopy し、`$explanation: () -> CallExplanation` パラメータを追加
  - `@JvmSynthetic` アノテーションを付与
  - overridden symbols も再帰的に変換
  - メタデータに power-assert 情報を埋め込み（別コンパイルユニットからの呼び出しに対応）

#### 診断メッセージ (PowerAssertDiagnostics)
- **エラー (3)**:
  - `POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS` - `@PowerAssert` 関数外での `explanation` アクセス
  - `POWER_ASSERT_ILLEGAL_OVERRIDE` - override 関数への `@PowerAssert` 不正使用
  - `POWER_ASSERT_ILLEGAL_ACTUAL` - actual 関数への `@PowerAssert` 不正使用
- **警告 (3)**:
  - `POWER_ASSERT_RUNTIME_UNAVAILABLE` - ランタイムライブラリ不在
  - `POWER_ASSERT_FUNCTION_NOT_TRANSFORMED` - 関数が変換されていない
  - `POWER_ASSERT_CAPABLE_OVERLOAD_MISSING` - 適切なオーバーロードが存在しない
- **情報 (1)**:
  - `POWER_ASSERT_CONSTANT` - 定数式のみ

---

## kapt

### 登録エントリポイント
- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/kapt/kapt-compiler/src/org/jetbrains/kotlin/kapt/KaptCompilerPluginRegistrar.kt
- 登録内容:
  - `FirAnalysisHandlerExtension` → `FirKaptAnalysisHandlerExtension`
- 備考: K2 のみサポート（`supportsK2 = true`）。K1 用の旧 registrar (`KaptPlugin`) は別途 `KaptPlugin.kt` に存在

### 機能一覧

| クラス名 | FIR/IR | K2/K1 | ソースURL |
|---|---|---|---|
| KaptCompilerPluginRegistrar | - | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/kapt/kapt-compiler/src/org/jetbrains/kotlin/kapt/KaptCompilerPluginRegistrar.kt) |
| FirKaptAnalysisHandlerExtension | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/kapt/kapt-compiler/src/org/jetbrains/kotlin/kapt/FirKaptAnalysisHandlerExtension.kt) |
| KaptPlugin (CommandLineProcessor) | - | K1 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/kapt/kapt-compiler/src/org/jetbrains/kotlin/kapt/KaptPlugin.kt) |
| KaptStubConverter | - | 共通 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/kapt/kapt-compiler/src/org/jetbrains/kotlin/kapt/stubs/KaptStubConverter.kt) |

### 各機能の詳細

#### FirKaptAnalysisHandlerExtension (FIR / K2)
- **継承/実装**: `FirAnalysisHandlerExtension`
- **オーバーライドメソッド**:
  - `isApplicable(configuration)` - KAPT オプションが設定されており skipBodies モードでない場合に適用
  - `doAnalysis(project, configuration)` - メイン処理
- **動作の詳細**:
  KAPT は他のプラグインと大きく異なり、**FIR/IR Extension としてコード変換を行うのではなく、コンパイラパイプライン全体を内部で再実行**する:
  1. **スタブ生成フェーズ** (`options.mode.generateStubs` の場合):
     - `skipBodies = true` モードで Kotlin コンパイラのフルパイプラインを内部実行
     - `JvmFrontendPipelinePhase` → `JvmFir2IrPipelinePhase` → `JvmBackendPipelinePhase` を順次実行
     - `OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)` でバイトコード生成をインターセプト
     - `KaptStubConverter` でインメモリバイトコードから Java ソーススタブを生成
     - スタブファイルを出力ディレクトリに書き込み
  2. **アノテーション処理フェーズ** (`options.mode.runAnnotationProcessing` の場合):
     - `ProcessorLoader` で javax.annotation.processing.Processor を読み込み
     - `KaptContext.doAnnotationProcessing()` で Java アノテーションプロセッサを実行
     - メモリリーク検出（オプション）
  3. **制約**:
     - `WITH_COMPILATION` モードは K2 では非サポート（エラー報告）
     - 構文エラーのみ手動チェック（その他の診断は抑制）
     - 各パイプラインフェーズで個別の `DiagnosticsCollectorImpl` を使用（メインパイプラインへのエラー伝播を防止）
- **診断メッセージ**:
  - "KAPT compile mode is not supported in Kotlin 2.x"
  - 構文エラーのみ報告（`FirSyntaxErrors.SYNTAX`）
  - KaptBaseError / KaptError による例外ベースのエラーハンドリング

---

## js-plain-objects

### 登録エントリポイント
- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.cli/src/org/jetbrains/kotlinx/jso/compiler/cli/JsPlainObjectsComponentRegistrar.kt
- 登録内容:
  - `FirExtensionRegistrar` → `JsPlainObjectsExtensionRegistrar`
  - `IrGenerationExtension` → `JsPlainObjectsLoweringExtension`
- pluginId: `"org.jetbrains.kotlinx.jspo"`

### 機能一覧

| クラス名 | FIR/IR | K2/K1 | ソースURL |
|---|---|---|---|
| JsPlainObjectsExtensionRegistrar | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/JsPlainObjectsExtensionRegistrar.kt) |
| JsPlainObjectsFunctionsGenerator | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/JsPlainObjectsFunctionsGenerator.kt) |
| FirJsPlainObjectsCheckersComponent | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/checkers/FirJsPlainObjectsCheckersComponent.kt) |
| FirJsPlainObjectsPluginClassChecker | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/checkers/FirJsPlainObjectsPluginClassChecker.kt) |
| JsPlainObjectsPropertiesProvider | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/services/JsPlainObjectsPropertiesProvider.kt) |
| JsPlainObjectsLoweringExtension | IR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.backend/src/org/jetbrains/kotlinx/jso/compiler/backend/JsObjectLoweringExtension.kt) |
| FirJsPlainObjectsErrors (診断定義) | FIR | K2 | [link](https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/checkers/FirJsPlainObjectsErrors.kt) |

### 各機能の詳細

#### JsPlainObjectsExtensionRegistrar (FIR / K2)
- **継承/実装**: `FirExtensionRegistrar`
- **オーバーライドメソッド**: `configurePlugin()`
- **動作の詳細**:
  - `FirJsPlainObjectsCheckersComponent` (チェッカー) を登録
  - `JsPlainObjectsFunctionsGenerator` (宣言生成) を登録
  - `JsPlainObjectsPropertiesProvider` (FIR セッションサービス) を登録
  - `FirJsPlainObjectsErrors` 診断コンテナを登録

#### JsPlainObjectsFunctionsGenerator (FIR / K2)
- **継承/実装**: `FirDeclarationGenerationExtension`
- **主要メソッド**:
  - `generateNestedClassLikeDeclaration()` - companion object を生成
  - `generateFunctions()` - `invoke` (ファクトリ) と `copy` 関数を生成
  - `getCallableNamesForClass()` / `getNestedClassifiersNames()` - 生成対象の名前セットを返す
- **動作の詳細**:
  - `@JsPlainObject` アノテーション付き external interface に対して:
    1. **companion object** を自動生成（存在しない場合）
    2. **`invoke` operator 関数** をコンパニオンに生成 - インターフェースのプロパティをパラメータとしたファクトリ関数。nullable プロパティには `VOID` デフォルト値
    3. **`copy` 関数** をコンパニオンに生成 - 元インスタンスと変更プロパティを受け取るコピー関数
  - 生成関数は `inline` で `@JsNoDispatchReceiver` アノテーション付き
  - `@JsName` アノテーションを使用して JS プロパティ名マッピングを処理
  - 型パラメータ対応

#### FirJsPlainObjectsCheckersComponent (FIR / K2)
- **継承/実装**: `FirAdditionalCheckersExtension`
- **登録チェッカー**:
  - `declarationCheckers.classCheckers` → `FirJsPlainObjectsPluginClassChecker`

#### FirJsPlainObjectsPluginClassChecker (FIR / K2)
- **継承/実装**: `FirClassChecker`
- **チェック内容**:
  - `@JsPlainObject` が external interface にのみ適用されていること
  - インターフェース内にメソッドが含まれていないこと（`Any` のメソッドと inline 関数を除く）
  - `@JsPlainObject` インターフェースは他の `@JsPlainObject` インターフェースのみ継承可能
  - 非アノテーションクラスが `@JsPlainObject` インターフェースを実装していないこと

#### JsPlainObjectsPropertiesProvider (FIR / K2)
- **継承/実装**: `FirExtensionSessionComponent`
- **動作の詳細**:
  - `@JsPlainObject` クラスのプロパティ情報をキャッシュするセッションコンポーネント
  - スーパータイプの解決、型パラメータの置換を行い、プロパティリストを構築
  - `@JsName` アノテーションからの JS 名を抽出
  - `JsPlainObjectsFunctionsGenerator` から利用される

#### JsPlainObjectsLoweringExtension (IR / K2)
- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**:
  - `MoveExternalInlineFunctionsWithBodiesOutsideLowering` を適用
  - external inline 関数（`invoke`, `copy`）の実装をトップレベル関数に移動
  - ファクトリ関数: JS オブジェクトリテラル (`js("{ ... }")`) を生成してプロパティを設定
  - コピー関数: ソースオブジェクトのプロパティをコピーし、指定パラメータで上書き
  - 元の関数ボディはプロキシ呼び出しに置換
  - ES5 互換の識別子サニタイズ処理

#### 診断メッセージ (FirJsPlainObjectsErrors)
- `NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED` - external でない宣言には非対応
- `ONLY_INTERFACES_ARE_SUPPORTED` - インターフェースのみ対応
- `IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED` - JsPlainObject の実装は非対応
- `METHODS_ARE_NOT_ALLOWED_INSIDE_JS_PLAIN_OBJECT` - メソッド定義不可
- `JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS` - 他の JsPlainObject のみ継承可

---

## 横断的まとめ

### Extension 種別の使い分けパターン

| Extension 種別 | 用途 | 使用プラグイン |
|---|---|---|
| `FirExtensionRegistrar` | FIR Extension の登録エントリポイント | parcelize, power-assert, js-plain-objects |
| `FirDeclarationGenerationExtension` | FIR レベルでの宣言（関数/クラス）自動生成 | parcelize, js-plain-objects |
| `FirAdditionalCheckersExtension` | FIR レベルでの追加バリデーション | parcelize, power-assert, js-plain-objects |
| `FirAnalysisHandlerExtension` | 分析フェーズへのフック（パイプライン制御） | kapt |
| `IrGenerationExtension` | IR レベルでのコード変換・生成 | parcelize, power-assert, js-plain-objects |
| `SyntheticResolveExtension` | K1 合成メンバ解決 | parcelize (K1のみ) |
| `StorageComponentContainerContributor` | K1 チェッカー登録 | parcelize (K1のみ) |

### K1 vs K2 対応状況

| プラグイン | K1 対応 | K2 対応 |
|---|---|---|
| parcelize | SyntheticResolveExtension + DeclarationChecker + IR | FirDeclarationGenerator + FirCheckers + IR |
| power-assert | IR のみ (FIR チェッカーなし) | FirCheckers + IR |
| kapt | AnalysisHandlerExtension (旧) | FirAnalysisHandlerExtension |
| js-plain-objects | K1 対応なし | FirDeclarationGenerator + FirCheckers + IR |
