# debuglog / zipline / arrow / back-in-time / kondition / suspend-kontext / AspectK 詳細

## debuglog

### 登録エントリポイント

- ファイル: `https://github.com/bnorm/debuglog/blob/main/debuglog-plugin/src/main/kotlin/com/bnorm/debug/log/DebugLogComponentRegistrar.kt`
- 登録クラス: `DebugLogComponentRegistrar` (`ComponentRegistrar` — K1 世代の旧 API)
- 登録内容:
  - `IrGenerationExtension` → `DebugLogIrGenerationExtension`
  - `enabled` オプションが false の場合は登録しない
- CLIオプション管理: `DebugLogCommandLineProcessor` (`@AutoService(CommandLineProcessor::class)`)
  - オプション: `enabled` (bool)

### 各機能の詳細

#### DebugLogCommandLineProcessor (K1/CommandLineProcessor)

- **継承/実装**: `CommandLineProcessor`
- **オーバーライドメソッド**: `processOption`
- **動作の詳細**: `enabled` オプション値を `CompilerConfiguration` に格納する

#### DebugLogIrGenerationExtension (IR / K1)

- **継承/実装**: `IrGenerationExtension`
- **ファイル**: `https://github.com/bnorm/debuglog/blob/main/debuglog-plugin/src/main/kotlin/com/bnorm/debug/log/DebugLogIrGenerationExtension.kt`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**:
  - `@DebugLog` アノテーションクラスのシンボルを取得
  - `kotlin.io.println` の単一引数版を取得
  - `DebugLogTransformer` を `moduleFragment` に適用

#### DebugLogTransformer (IR / K1)

- **継承/実装**: `IrElementTransformerVoidWithContext`
- **ファイル**: `https://github.com/bnorm/debuglog/blob/main/debuglog-plugin/src/main/kotlin/com/bnorm/debug/log/DebugLogTransformer.kt`
- **オーバーライドメソッド**: `visitFunctionNew(declaration)`
- **動作の詳細**:
  - `@DebugLog` が付いた関数を検出し、本体を変換する
  - 関数の先頭に `⇢ functionName(param1=..., param2=...)` 形式の `println` を挿入
  - `TimeSource.Monotonic.markNow()` で開始時刻を記録
  - 関数本体を `try/catch` でラップ
  - 正常終了時: `return` 文を書き換え、`⇠ functionName [elapsed] = result` 形式の `println` を挿入してからリターン
  - 例外時: キャッチブロックで `⇠ functionName [elapsed]` を出力し再スロー
  - `DebugLogReturnTransformer` (inner class) で `return` 式を再帰的に書き換え

---

## zipline

### 登録エントリポイント

- ファイル: `https://github.com/cashapp/zipline/blob/main/zipline-kotlin-plugin/src/main/kotlin/app/cash/zipline/kotlin/ZiplineCompilerPluginRegistrar.kt`
- 登録クラス: `ZiplineCompilerPluginRegistrar` (`CompilerPluginRegistrar` — K2 対応新 API、`supportsK2 = true`)
- 登録内容:
  - `IrGenerationExtension` → `ZiplineIrGenerationExtension`
- CLIオプション管理: `ZiplineCommandLineProcessor` (オプションなし)

### 各機能の詳細

#### ZiplineIrGenerationExtension (IR / K2対応)

- **継承/実装**: `IrGenerationExtension`
- **ファイル**: `https://github.com/cashapp/zipline/blob/main/zipline-kotlin-plugin/src/main/kotlin/app/cash/zipline/kotlin/ZiplineIrGenerationExtension.kt`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**: 匿名の `IrElementTransformerVoidWithContext` を使い2種類の変換を行う
  1. **`visitClassNew`**: `ZiplineService` を継承するインターフェースを検出し、`AdapterGenerator.generateAdapterIfAbsent()` を呼び出してコンパニオンオブジェクトへ `Adapter` ネストクラスを生成する。`ZiplineScoped` をインターフェースが implement していたらエラー
  2. **`visitCall`**: `Zipline.take()` / `Zipline.bind()` などへの呼び出しを検出し `AddAdapterArgumentRewriter` でアダプタ引数を自動補完、`ziplineServiceSerializer()` 呼び出しを `CallAdapterConstructorRewriter` でコンストラクタ呼び出しに書き換える

#### AdapterGenerator (IR / K2対応)

- **ファイル**: `https://github.com/cashapp/zipline/blob/main/zipline-kotlin-plugin/src/main/kotlin/app/cash/zipline/kotlin/AdapterGenerator.kt`
- **動作の詳細**: `ZiplineService` インターフェースのコンパニオンオブジェクトに `internal class Adapter` を生成する。生成内容:
  - `serialName`/`simpleName`/`serializers` プロパティのオーバーライド
  - インターフェースの各メソッドごとに `ZiplineFunction0`, `ZiplineFunction1`, ... 内部クラスを生成し、`call`/`callSuspending` をオーバーライド
  - `ziplineFunctions()` 関数のオーバーライド (全 ZiplineFunction のリストを返す)
  - `outboundService()` 関数のオーバーライド + `GeneratedOutboundService` 内部クラスの生成

---

## arrow (Arrow Optics Compiler Plugin)

### 登録エントリポイント

- ファイル: `https://github.com/arrow-kt/arrow/blob/main/arrow-libs/optics/arrow-optics-compiler-plugin/src/main/kotlin/arrow/optics/plugin/fir/OpticsPluginWrappers.kt`
- 登録クラス: `OpticsPluginComponentRegistrar` (`CompilerPluginRegistrar`、`supportsK2 = true`)
- 登録内容:
  - `FirExtensionRegistrarAdapter` → `OpticsPluginRegistrar` → `OpticsCompanionGenerator`
- CLIオプション管理: `OpticsCommandLineProcessor` (オプションなし)

### 各機能の詳細

#### OpticsCompanionGenerator (FIR / K2)

- **継承/実装**: `FirDeclarationGenerationExtension`
- **ファイル**: `https://github.com/arrow-kt/arrow/blob/main/arrow-libs/optics/arrow-optics-compiler-plugin/src/main/kotlin/arrow/optics/plugin/fir/OpticsCompanionGenerator.kt`
- **オーバーライドメソッド**:
  - `FirDeclarationPredicateRegistrar.registerPredicates()`
  - `getNestedClassifiersNames(classSymbol, context)`
  - `generateNestedClassLikeDeclaration(owner, name, context)`
  - `getCallableNamesForClass(classSymbol, context)`
  - `generateConstructors(context)`
- **動作の詳細**:
  - `@optics` アノテーションが付いたクラスを Predicate でフィルタリング
  - コンパニオンオブジェクトがない場合、FIR フェーズで `companion object` を生成する (`createCompanionObject`)
  - 生成したコンパニオンオブジェクトに private コンストラクタを生成 (`createDefaultPrivateConstructor`)
  - 実際の `Lens`, `Optional` 等のプロパティ生成は IR フェーズ(別実装)が担当する模様
- **注記**: K2 FIR のみの実装。optics の実際のコード生成 (Lens/Prism/Optional 等) は KSP 側の処理か IR フェーズに移譲されている

---

## back-in-time-plugin

### 登録エントリポイント

- ファイル: `https://github.com/kitakkun/back-in-time-plugin/blob/main/compiler/cli/src/main/kotlin/com/kitakkun/backintime/compiler/cli/BackInTimeCompilerRegistrar.kt`
- 登録クラス: `BackInTimeCompilerRegistrar` (`CompilerPluginRegistrar`、`supportsK2 = true`)
- 登録内容:
  - `FirExtensionRegistrarAdapter` → `BackInTimeFirExtensionRegistrar`
    - `BackInTimeYamlConfigurationProvider` (FIR セッションコンポーネント)
    - `BackInTimeFirSupertypeGenerationExtension`
    - `BackInTimeFirDeclarationGenerationExtension`
    - `BackInTimeFirAdditionalCheckersExtension`
  - `IrGenerationExtension` → `BackInTimeIrGenerationExtension`
- 設定: YAML ファイルでトラッキング対象の状態ホルダーを設定可能

### 各機能の詳細

#### BackInTimeFirSupertypeGenerationExtension (FIR / K2)

- **継承/実装**: `FirSupertypeGenerationExtension`
- **ファイル**: `https://github.com/kitakkun/back-in-time-plugin/blob/main/compiler/k2/src/main/kotlin/com/kitakkun/backintime/compiler/k2/extension/BackInTimeFirSupertypeGenerationExtension.kt`
- **オーバーライドメソッド**:
  - `needTransformSupertypes(declaration)`
  - `computeAdditionalSupertypes(classLikeDeclaration, resolvedSupertypes, typeResolver)`
  - `FirDeclarationPredicateRegistrar.registerPredicates()`
- **動作の詳細**: `BackInTimePredicate` に合致するクラス (`@BackInTimeDebuggable` など) に `BackInTimeDebuggable` インターフェースをスーパータイプとして自動追加する

#### BackInTimeFirDeclarationGenerationExtension (FIR / K2)

- **継承/実装**: `FirDeclarationGenerationExtension`
- **ファイル**: `https://github.com/kitakkun/back-in-time-plugin/blob/main/compiler/k2/src/main/kotlin/com/kitakkun/backintime/compiler/k2/extension/BackInTimeFirDeclarationGenerationExtension.kt`
- **オーバーライドメソッド**:
  - `getCallableNamesForClass(classSymbol, context)` → `forceSetValue`, `backInTimeInstanceUUID`, `backInTimeInitializedPropertyMap` を返す
  - `generateProperties(callableId, context)` → `BackInTimeDebuggable` インターフェースから対応プロパティを複製して生成
  - `generateFunctions(callableId, context)` → `BackInTimeDebuggable` インターフェースから対応関数を複製して生成
- **動作の詳細**: 対象クラスに `BackInTimeDebuggable` の実装メンバー (UUID プロパティ、初期化状態マップ、強制値設定メソッド) を FIR レベルで宣言する

#### BackInTimeFirAdditionalCheckersExtension (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension`
- **ファイル**: `https://github.com/kitakkun/back-in-time-plugin/blob/main/compiler/k2/src/main/kotlin/com/kitakkun/backintime/compiler/k2/checkers/BackInTimeFirAdditionalCheckersExtension.kt`
- **オーバーライドメソッド**: `declarationCheckers` (regularClassCheckers)
- **登録チェッカー**:
  - `TrackableStateHolderDefinitionChecker` — YAML で定義されたトラッキング対象の状態ホルダーが正しく定義されているか確認
  - `BackInTimeTargetClassPropertyChecker` — ターゲットクラスのプロパティに対する各種バリデーション
- **診断メッセージ** (`FirBackInTimeErrors`):
  - `MULTIPLE_PROPERTY_SETTER` (warning) — セッターが複数ある
  - `MULTIPLE_PROPERTY_GETTER` (warning) — ゲッターが複数ある
  - `MISSING_CAPTURE_CALL` (warning) — キャプチャ呼び出しがない
  - `MISSING_PROPERTY_SETTER` (warning) — セッターがない
  - `MISSING_PROPERTY_GETTER` (warning) — ゲッターがない
  - `VALUE_CONTAINER_MORE_THAN_TWO_TYPE_ARGUMENTS` (warning) — 型引数が2つ以上
  - `PROPERTY_VALUE_MUST_BE_SERIALIZABLE` (warning) — 値がシリアライズ可能でない

#### BackInTimeIrGenerationExtension (IR / K2対応)

- **継承/実装**: `IrGenerationExtension`
- **ファイル**: `https://github.com/kitakkun/back-in-time-plugin/blob/main/compiler/backend/src/main/kotlin/com/kitakkun/backintime/compiler/backend/BackInTimeIrGenerationExtension.kt`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**: 5つのトランスフォーマーを順番に適用する
  1. `BackInTimeEntryPointTransformer` — エントリポイントの変換
  2. `BackInTimeDebuggableConstructorTransformer` — コンストラクタにメソッド呼び出し通知とプロパティキャプチャを追加
  3. `BackInTimeDebuggableCapturePropertyChangesTransformer` — プロパティ変更時のキャプチャ処理を各関数・セッター呼び出しに注入
  4. `BackInTimeDebuggableImplementTransformer` — FIR で宣言されたメンバーの実装本体を生成 (`forceSetValue` の when 式、UUID プロパティの初期値など)
  5. `BackInTimeDebuggableCaptureLazyDebuggablePropertyAccessTransformer` — Lazy な debuggable プロパティへのアクセスのキャプチャ

#### BackInTimeDebuggableImplementTransformer (IR / K2対応)

- **継承/実装**: `IrElementTransformerVoid`
- **ファイル**: `https://github.com/kitakkun/back-in-time-plugin/blob/main/compiler/backend/src/main/kotlin/com/kitakkun/backintime/compiler/backend/transformer/implement/BackInTimeDebuggableImplementTransformer.kt`
- **オーバーライドメソッド**: `visitSimpleFunction`, `visitProperty`
- **動作の詳細**:
  - `forceSetValue` メソッド: `when (propertySignature)` 式を生成し、各プロパティへの値の強制設定 (JSON デシリアライズ後にセッター呼び出し) を実装。スーパークラスが `BackInTimeDebuggable` なら `super.forceSetValue()` へのフォールバックも追加
  - `backInTimeInstanceUUID` プロパティ: バッキングフィールドを追加し `uuid()` 関数呼び出しで初期化
  - `backInTimeInitializedPropertyMap` プロパティ: `mutableMapOf<String, Boolean>()` で初期化するバッキングフィールドを追加

#### BackInTimeDebuggableCapturePropertyChangesTransformer (IR / K2対応)

- **継承/実装**: `IrElementTransformerVoidWithContext`
- **ファイル**: `https://github.com/kitakkun/back-in-time-plugin/blob/main/compiler/backend/src/main/kotlin/com/kitakkun/backintime/compiler/backend/transformer/capture/BackInTimeDebuggableCapturePropertyChangesTransformer.kt`
- **オーバーライドメソッド**: `visitConstructor`, `visitFunctionNew`, `visitCall`
- **動作の詳細**:
  - コンストラクタ: UUID 変数を生成し、`reportMethodInvocation` 呼び出しと全プロパティの初期値キャプチャを本体末尾に追加
  - 通常関数: 先頭に UUID 変数と `reportMethodInvocation` を追加
  - `visitCall`: セッター呼び出しや TrackableStateHolder の setter/関連メソッド呼び出しを検出し、値変更通知のキャプチャコードを挿入

---

## kondition

### 登録エントリポイント

- ファイル: `https://github.com/kitakkun/Kondition/blob/main/compiler/cli/src/main/kotlin/com/kitakkun/kondition/compiler/cli/KonditionCompilerPluginRegistrar.kt`
- 登録クラス: `KonditionCompilerPluginRegistrar` (`CompilerPluginRegistrar`、`supportsK2 = true`)
- 登録内容:
  - `FirExtensionRegistrarAdapter` → `KonditionFirExtensionRegistrar` → `KonditionFirCheckersExtension`
  - `IrGenerationExtension` → `KonditionIrGenerationExtension`

### 各機能の詳細

#### KonditionFirCheckersExtension (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension`
- **ファイル**: `https://github.com/kitakkun/Kondition/blob/main/compiler/k2/src/main/kotlin/com/kitakkun/kondition/compiler/k2/extensions/KonditionFirCheckersExtension.kt`
- **登録チェッカー**:
  - `declarationCheckers`:
    - `AbortStrategyChecker` (`FirFunctionChecker`) — `AbortStrategy` の設定が適切か確認
    - `NonApplicableAnnotationUsageChecker` (`FirCallableDeclarationChecker`) — 対象型に適用不可のアノテーションが使われていないか
  - `expressionCheckers`:
    - `OutOfRangeValueChecker` (`FirAnnotationChecker`) — アノテーションの数値引数が定義可能範囲外でないか
    - `InvalidRangeChecker` (`FirAnnotationChecker`) — 範囲アノテーション (min > max など) の無効な組み合わせ
- **診断メッセージ** (`KonditionErrors`):
  - `RETURN_IMPOSSIBLE_FOR_NON_UNIT_TYPE` (error1: String)
  - `OUT_OF_RANGE` (error2: Number, ClassId)
  - `KONDITION_ANNOTATION_USED_AGAINST_NON_APPLICABLE_TYPE` (error3: ClassId, ClassId, List<ClassId>)
  - `INVALID_RANGE` (error2: String, ClassId)

#### KonditionIrGenerationExtension (IR / K2対応)

- **継承/実装**: `IrGenerationExtension`
- **ファイル**: `https://github.com/kitakkun/Kondition/blob/main/compiler/backend/src/main/kotlin/com/kitakkun/kondition/compiler/backend/KonditionIrGenerationExtension.kt`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**: `RequirementProvider` と `ValueFitter` のリストを組み立て、2つのトランスフォーマーを適用する
  1. `ValueParameterCheckStatementsProducer` — 関数パラメータへのバリデーション/フィッティング処理を本体先頭に挿入
  2. `LocalVariablesCheckProducer` — ローカル変数に対する同様の処理

#### ValueParameterCheckStatementsProducer (IR / K2対応)

- **継承/実装**: `IrElementTransformerVoid`
- **ファイル**: `https://github.com/kitakkun/Kondition/blob/main/compiler/backend/src/main/kotlin/com/kitakkun/kondition/compiler/backend/transformer/ValueParameterCheckStatementsProducer.kt`
- **オーバーライドメソッド**: `visitFunction`, `visitField`
- **動作の詳細**:
  - `visitFunction`: 各バリューパラメータのアノテーションを検査し、`StatementsProducer` で `require(condition) { ... }` 系のチェック文を生成して関数本体先頭に挿入。コンストラクタ以外では `FitValueProducer` でフィッティング済み一時変数も生成
  - `visitField`: コンストラクタパラメータからフィールド初期化 (`INITIALIZE_PROPERTY_FROM_PARAMETER`) の場合、フィッティング処理を初期化式に適用
- **RequirementProviders**: `NonEmpty`, `NonBlank`, `MatchRegex`, `Alphabetic`, `Numeric`, `Length`, `MinLength`, `MaxLength`, `Prefixed`, `Suffixed`, `UpperCased`, `LowerCased`, `RangedLong`, `RangedDecimal`, `GreaterThan`/`LessThan`/`...OrEquals` (Long/Decimal), `Positive`, `NonPositive`, `Negative`, `NonNegative`, `NonZero` など
- **ValueFitters**: `CoerceAtLeast`/`CoerceAtMost`/`CoerceIn` (Long/Decimal), `RemovePrefix`, `AddPrefix`, `RemoveSuffix`, `AddSuffix`, `Trim`/`TrimStart`/`TrimEnd`, `Take`/`TakeLast`/`Drop`/`DropLast`, `ToUpperCase`, `ToLowerCase`

---

## suspend-kontext

### 登録エントリポイント

- ファイル: `https://github.com/kitakkun/suspend-kontext/blob/main/compiler/cli/src/main/core/com/kitakkun/suspendkontext/compiler/cli/SuspendKontextCompilerPluginRegistrar.kt`
- 登録クラス: `SuspendKontextCompilerPluginRegistrar` (`CompilerPluginRegistrar`、`supportsK2 = true`)
- 登録内容:
  - `FirExtensionRegistrarAdapter` → `SuspendKontextFirExtensionRegistrar` → `SuspendKontextFirCheckersExtension`
  - `IrGenerationExtension` → `SuspendKontextIrGenerationExtension`
- 注記: Kotlin バージョン差異対応のため `VersionSpecificApi` / `VersionSpecificApiImpl` を `core` と `latest`/`pre_2_0_21` ソースセットで切り替えている

### 各機能の詳細

#### SuspendKontextFirCheckersExtension (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension`
- **ファイル**: `https://github.com/kitakkun/suspend-kontext/blob/main/compiler/k2/src/main/core/com/kitakkun/suspendkontext/compiler/k2/extension/SuspendKontextFirCheckersExtension.kt`
- **登録チェッカー**:
  - `declarationCheckers.functionCheckers`:
    - `AnnotationUseSiteChecker` — アノテーションが suspend 関数にのみ使用されているか確認
    - `MultipleContextApplicationChecker` — 同じ関数に複数のコンテキストアノテーションが付いていないか
  - `expressionCheckers.annotationCallCheckers`:
    - `CustomContextDefinitionChecker` — `@CustomContext` アノテーションが Dispatcher クラスを正しく参照しているか
- **診断メッセージ** (`SuspendKontextErrors`):
  - `NON_SUSPEND_FUNCTION` (error0) — suspend でない関数への使用
  - `NON_DISPATCHER_CLASS` (error0) — Dispatcher でないクラスの指定
  - `MULTIPLE_CONTEXT_APPLICATION` (error0) — 複数コンテキストの同時指定

#### SuspendKontextIrGenerationExtension (IR / K2対応)

- **継承/実装**: `IrGenerationExtension`
- **ファイル**: `https://github.com/kitakkun/suspend-kontext/blob/main/compiler/backend/src/main/core/com/kitakkun/suspendkontext/compiler/backend/SuspendKontextIrGenerationExtension.kt`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**: `CoroutineContextTransformer` を `moduleFragment` に適用する

#### CoroutineContextTransformer (IR / K2対応)

- **継承/実装**: `IrElementTransformerVoid`
- **ファイル**: `https://github.com/kitakkun/suspend-kontext/blob/main/compiler/backend/src/main/core/com/kitakkun/suspendkontext/compiler/backend/CoroutineContextTransformer.kt`
- **オーバーライドメソッド**: `visitFunction(declaration)`
- **動作の詳細**:
  - `@IoContext`, `@DefaultContext`, `@UnconfinedContext`, `@MainContext`, `@CustomContext` のいずれかが付いた suspend 関数を検出
  - 関数の本体を匿名 lambda 関数に移し、`withContext(dispatcher) { ... }` でラップした新しい本体に置き換える
  - `@CustomContext` の場合は、アノテーション引数に指定されたクラスをインスタンス化して dispatcher として使用 (object なら `getObject`、それ以外は primary constructor 呼び出し)
  - 標準アノテーションの場合は `Dispatchers.IO` / `Dispatchers.Default` / `Dispatchers.Unconfined` / `Dispatchers.Main` を使用

---

## AspectK

### 登録エントリポイント

- ファイル: `https://github.com/kitakkun/AspectK/blob/main/aspectk-compiler/src/main/kotlin/com/github/kitakkun/aspectk/compiler/AspectKCompilerPluginRegistrar.kt`
- 登録クラス: `AspectKCompilerPluginRegistrar` (`CompilerPluginRegistrar`、`supportsK2 = true`、`@AutoService`)
- 登録内容:
  - `FirExtensionRegistrarAdapter` → `AspectKFirExtensionRegistrar` → `AspectKFirCheckerExtension`
  - `IrGenerationExtension` → `AspectKIrGenerationExtension`
  - `enabled` が false の場合は何も登録しない

### 各機能の詳細

#### AspectKFirCheckerExtension (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension`
- **ファイル**: `https://github.com/kitakkun/AspectK/blob/main/aspectk-compiler/src/main/kotlin/com/github/kitakkun/aspectk/compiler/fir/checker/AspectKFirCheckerExtension.kt`
- **登録チェッカー**:
  - `declarationCheckers.functionCheckers`: `AdviceOrPointcutFunctionChecker` — advice/pointcut 関数が `@Aspect` クラス内でのみ宣言されているか確認
  - `declarationCheckers.classCheckers`: `AspectClassChecker` — `@Aspect` クラスが正しい構造を持っているか確認
- **診断メッセージ** (`AspectKErrors`):
  - `POINTCUT_FUNCTION_DECLARATION_SCOPE_VIOLATION` (error0) — pointcut 関数のスコープ違反
  - `ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION` (error1: String) — advice 関数のスコープ違反
  - `EMPTY_POINTCUT_EXPRESSION` (warning0) — 空の pointcut 式
  - `ASPECT_CLASS_WITH_NO_POINTCUT_OR_ADVICE_ENTRIES` (warning0) — エントリなしの aspect クラス
  - `INVALID_POINTCUT_EXPRESSION` (error1: String) — 無効な pointcut 式

#### AspectKIrGenerationExtension (IR / K2対応)

- **継承/実装**: `IrGenerationExtension`
- **ファイル**: `https://github.com/kitakkun/AspectK/blob/main/aspectk-compiler/src/main/kotlin/com/github/kitakkun/aspectk/compiler/backend/AspectKIrGenerationExtension.kt`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**:
  1. `AspectAnalyzer.analyze(moduleFragment)` で `@Aspect` アノテーション付きクラスを全収集し、各クラスの `@Pointcut` / `@Before` / `@After` / `@Around` を解析
  2. `AspectKTransformer` を適用

#### AspectAnalyzer (IR / K2対応)

- **継承/実装**: `IrElementVisitorVoid`
- **ファイル**: `https://github.com/kitakkun/AspectK/blob/main/aspectk-compiler/src/main/kotlin/com/github/kitakkun/aspectk/compiler/backend/analyzer/AspectAnalyzer.kt`
- **動作の詳細**: モジュール内の全クラスを走査し、`@Aspect` 付きクラスを見つけたら `analyzeClass()` で以下を収集する
  - `@Pointcut` 付き関数 → `PointcutExpressionParser` で式をパース
  - `@Before` / `@After` / `@Around` 付き関数 → 同様にパースして `Advice` オブジェクトに

#### AspectKTransformer (IR / K2対応)

- **継承/実装**: `IrElementTransformerVoid` (K2 context parameters 機能を使い `context(AspectKIrPluginContext)` で拡張)
- **ファイル**: `https://github.com/kitakkun/AspectK/blob/main/aspectk-compiler/src/main/kotlin/com/github/kitakkun/aspectk/compiler/backend/AspectKTransformer.kt`
- **オーバーライドメソッド**: `visitSimpleFunction(declaration)`
- **動作の詳細**:
  - `@Aspect`/`@Pointcut`/`@Before`/`@After`/`@Around` 付き関数はスキップ
  - それ以外の関数を `FunctionSpec` に変換し、全 `AspectClass` の advice に対して pointcut 式マッチングを行う
  - マッチした advice ごとに `ApplyAdviceTransformer` を適用

#### ApplyAdviceTransformer (IR / K2対応)

- **継承/実装**: `IrElementTransformerVoid` (context(AspectKIrPluginContext))
- **ファイル**: `https://github.com/kitakkun/AspectK/blob/main/aspectk-compiler/src/main/kotlin/com/github/kitakkun/aspectk/compiler/backend/transformer/ApplyAdviceTransformer.kt`
- **オーバーライドメソッド**: `visitSimpleFunction(declaration)`
- **動作の詳細**:
  1. aspect クラスのインスタンス変数を生成 (object なら `irGetObject`、それ以外は primary constructor 呼び出し)
  2. `JoinPoint(this, listOf(args...))` 変数を生成
  3. advice 関数呼び出し式を生成 (`JoinPoint` を引数に取れる場合は渡す)
  4. advice タイプに応じて適用:
     - `BEFORE`: 関数本体先頭に advice 呼び出しを挿入 (`BeforeAdviceTransformer`)
     - `AFTER`: 全 `return` 文の直前に advice 呼び出しを挿入 (`AfterAdviceFunctionBodyTransformer`)
     - `AROUND`: BEFORE と AFTER の両方を適用 (完全な around は TODO)
