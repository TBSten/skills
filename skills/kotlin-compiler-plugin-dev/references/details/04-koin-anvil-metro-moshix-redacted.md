# koin / anvil / metro / MoshiX / redacted 詳細

## koin

### 登録エントリポイント

- ファイル: `https://github.com/InsertKoinIO/koin-annotations/blob/main/koin-compiler-plugin/koin-compiler-plugin/src/org/koin/compiler/plugin/KoinPluginComponentRegistrar.kt`
- 登録内容:
  - `FirExtensionRegistrarAdapter.registerExtension(KoinPluginRegistrar())` — FIR フェーズ用 Extension の登録
  - `IrGenerationExtension.registerExtension(KoinIrExtension(...))` — IR フェーズ用 Extension の登録
  - `KoinPluginLogger.init(...)` でグローバルロガーを初期化（userLogs, debugLogs, unsafeDslChecks, skipDefaultValues, compileSafety, lookupTracker）

- `KoinCommandLineProcessor` (`CommandLineProcessor`) が CLI オプションを処理:
  - `koin.userLogs`, `koin.debugLogs`, `koin.unsafeDslChecks`, `koin.skipDefaultValues`, `koin.compileSafety`

- `KoinPluginRegistrar` (`FirExtensionRegistrar`) が FIR Extension を登録:
  - `+::KoinModuleFirGenerator`
  - `+::FirKoinLookupRecorder`

### 各機能の詳細

#### KoinModuleFirGenerator (FIR / K2)

- **継承/実装**: `FirDeclarationGenerationExtension`
- **オーバーライドメソッド**:
  - `FirDeclarationPredicateRegistrar.registerPredicates()` — `@Module`, `@Configuration` などのアノテーションを持つシンボルを検索する predicate を登録
  - `getTopLevelFunctionIds(packageFqName)` — 生成するトップレベル関数の ID を返す（`@Module` クラスの `.module()` 拡張関数、`@Configuration` 用 hint 関数）
  - `generateTopLevelFunctions(callableId)` — 実際の FIR 関数宣言を生成する
- **動作の詳細**:
  - `@Module` アノテーション付きクラスに対して `fun MyModule.module(): Module = ...` 形式の拡張関数の FIR シンボルを生成（ボディは IR フェーズで補完）
  - `@Configuration` 付きクラスには加えて `org.koin.plugin.hints` パッケージ内に hint 関数を生成（クロスモジュール発見のため）
  - KMP 対応: Kotlin 2.3.20 以降は全プラットフォームで synthetic file 名を使用した FIR トップレベル宣言生成が可能

#### FirKoinLookupRecorder (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension`
- **オーバーライドメソッド**:
  - `declarationCheckers` — `KoinApplicationLookupChecker` を `regularClassCheckers` に登録
  - `KoinApplicationLookupChecker.check(FirRegularClass)` — `@KoinApplication` 付きクラスを検査
- **動作の詳細**:
  - `@KoinApplication` アノテーション付きクラスを発見したとき、`LookupTracker` を使って IC (incremental compilation) 依存関係を記録する
  - hint 関数パッケージから `@Configuration` モジュールクラスを探し、`@KoinApplication` ファイルから各モジュールクラスへの lookup を `ScopeKind.PACKAGE` で記録する
  - これにより `@Configuration` モジュールが変更されたとき、IC が `@KoinApplication` ファイルの再コンパイルをトリガーする

#### KoinIrExtension (IR / K2)

- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**:
  - `generate(moduleFragment, pluginContext)` — IR 変換の全フェーズを順次実行
- **動作の詳細（フェーズ順）**:
  - **Phase 0**: `KoinHintTransformer` — FIR で生成された hint 関数のボディを生成
  - **Phase 1**: `KoinAnnotationProcessor` — `@Module`/`@ComponentScan`/`@Singleton`/`@Factory` を処理し、`fun MyModule.module() = module { single<A>(); factory<B>() }` のようなモジュール拡張関数ボディを生成
  - **Phase 2**: `KoinDSLTransformer` — `single<T>()` → `single(T::class, null) { T(get(), get()) }` 形式へ変換。DSL 定義とコールサイトを収集
  - **Phase 2.5**: `DslHintGenerator` — クロスモジュール発見のため DSL 定義ごとに hint 関数を生成
  - **Phase 3**: `KoinStartTransformer` — `startKoin<T> { }` を `startKoin { modules(listOf(Module1().module, ...)) }` へ変換
  - **Phase 3.1**: DSL のみの A3 グラフバリデーション（`startKoin { }` がある場合）
  - **Phase 3.5**: 収集したコールサイトのバリデーション
  - **Phase 3.6**: 依存モジュールからのコールサイト hint のバリデーション
  - **Phase 4**: `KoinMonitorTransformer` — `@Monitor` アノテーション付き関数を Kotzilla トレース呼び出しでラップ

---

## anvil

### 登録エントリポイント

- ファイル: `https://github.com/square/anvil/blob/main/compiler/src/main/java/com/squareup/anvil/compiler/AnvilComponentRegistrar.kt`
- 登録内容:
  - `IrGenerationExtension.registerExtension(IrContributionMerger(...))` — merging が有効な場合のみ登録
  - `AnalysisHandlerExtension.registerExtensionFirst(CodeGenerationExtension(...))` — コード生成 Extension を最優先で登録
  - ServiceLoader 経由で外部 `CodeGenerator` 実装を自動ロード
  - `ContributesSubcomponentHandlerGenerator` を特別ケースとして追加（`ClassScanner` が必要なため）

- `AnvilCommandLineProcessor` (`CommandLineProcessor`) が CLI オプションを処理:
  - `gradle-project-dir`, `gradle-build-dir`, `src-gen-dir`, `anvil-cache-dir`, `ir-merges-file`
  - `generate-dagger-factories`, `generate-dagger-factories-only`, `disable-component-merging`, `track-source-files`, `will-have-dagger-factories`

- **注意**: anvil は `CompilerPluginRegistrar` ではなく古い `ComponentRegistrar` を使用（K2 対応のため `supportsK2 = true` を設定）

### 各機能の詳細

#### CodeGenerationExtension (Analysis Phase / K1+K2)

- **継承/実装**: `AnalysisHandlerExtension`
- **オーバーライドメソッド**:
  - `doAnalysis(project, module, ...)` — 再コンパイル済みかどうかを確認し `AnalysisResult.EMPTY` を返す
  - `analysisCompleted(project, module, bindingTrace, files)` — コード生成のメインロジック
- **動作の詳細**:
  - 初回は `generateCode()` で全コードジェネレータを実行し、生成ファイルを `generatedDir` に出力
  - `RetryWithAdditionalRoots` を返すことでコンパイラに再解析を要求（生成コードを含む）
  - `trackSourceFiles` が有効な場合、`FileCacheOperations`/`GeneratedFileCache` でインクリメンタルコンパイル用のファイルキャッシュを管理
  - `syncGeneratedDir()` でキャッシュから欠落ファイルを復元したり、古いファイルを削除する

#### IrContributionMerger (IR / K1+K2)

- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**:
  - `generate(moduleFragment, pluginContext)` — `IrElementTransformerVoid` で全クラスを走査し、マージ処理を実行
- **動作の詳細**:
  - **Module merging**: `@MergeComponent`/`@MergeSubcomponent`/`@MergeModules` 付きクラスを発見し、クラスパス上の `@ContributesTo` 付きモジュールを収集して Dagger `@Component`/`@Module` アノテーションの `modules` 引数に追加
  - **Interface merging**: `@ContributesTo` が付いたインターフェースを `@MergeComponent`/`@MergeSubcomponent`/`@MergeInterfaces` 付きインターフェースのスーパータイプとして追加
  - `trackSourceFiles` が有効な場合、マージ結果を `irMergesFile` に書き出す（IC の都合でクラスファイルには反映されないため）
  - `shouldAlsoBeAppliedInKaptStubGenerationMode = true` でカプトスタブ生成モードでも動作

#### CodeGenerator (K1+K2 / code generation)

- **継承/実装**: `CodeGenerator`（API インターフェース）
- 各実装クラス（`ContributesBindingCodeGen`, `ContributesToCodeGen`, `ContributesSubcomponentCodeGen`, `ContributesMultibindingCodeGen`, など）が個別のアノテーションに対応
- **動作の詳細**:
  - `generateCode(codeGenDir, anvilModule, projectFiles)` を実装して KotlinPoet でソースコードを生成
  - `InjectConstructorFactoryCodeGen` — `@Inject` コンストラクタから Dagger `Factory` クラスを生成
  - `AssistedInjectCodeGen` / `AssistedFactoryCodeGen` — Assisted Inject サポート
  - `MembersInjectorCodeGen` — `@Inject` フィールド/メソッドの MembersInjector を生成
  - `ProvidesMethodFactoryCodeGen` — `@Provides` メソッドの Factory を生成

---

## metro

### 登録エントリポイント

- ファイル: `https://github.com/ZacSweers/metro/blob/main/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/MetroCompilerPluginRegistrar.kt`
- 登録内容:
  - `registerFirExtensionCompat(MetroFirExtensionRegistrar(...))` — FIR Extension の登録（IDE でも有効）
  - `registerIrExtensionCompat(MetroIrGenerationExtension(...))` — IR Extension の登録（CLI のみ）
  - IDE 判定: `LLFirSession` クラスの存在確認でリフレクション判定

- `MetroCommandLineProcessor` — `MetroOption.entries` から自動的に全オプションを登録

- `MetroFirExtensionRegistrar` (`FirExtensionRegistrar`) が FIR Extension を登録:
  - `+MetroFirBuiltIns.getFactory(...)` — セッションコンポーネント
  - `+::MetroFirCheckers` — FIR チェッカー群
  - `+supertypeGenerator("Supertypes - graph factory", ::GraphFactoryFirSupertypeGenerator)`
  - `+supertypeGenerator("Supertypes - contributed interfaces", { ContributedInterfaceSupertypeGenerator(...) })`
  - `+{ FirAccessorOverrideStatusTransformer(session, compatContext) }` — IDE 以外のみ
  - `+compositeDeclarationGenerator()` — 複合ジェネレータ（外部拡張 + ネイティブ）

### 各機能の詳細

#### MetroFirCheckers (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension`
- 多数の FIR チェッカーを登録:
  - `InjectConstructorChecker` — `@Inject` コンストラクタの制約チェック
  - `DependencyGraphChecker` / `DependencyGraphCreatorChecker` — `@DependencyGraph` 使用の検証
  - `AssistedInjectChecker` — アシストインジェクションの検証
  - `MultibindsChecker`, `MapKeyChecker` — マルチバインディングの検証
  - `AggregationChecker`, `MergedContributionChecker` — コントリビューション集約の検証
  - `BindingContainerClassChecker`, `BindingContainerCallableChecker` — バインディングコンテナの検証
  - `FunctionInjectionChecker`, `MembersInjectChecker` — 関数インジェクションの検証

#### InjectedClassFirGenerator (FIR / K2)

- **継承/実装**: `FirDeclarationGenerationExtension`
- **オーバーライドメソッド**:
  - `registerPredicates()` — `@Inject` アノテーション付きクラスを対象に predicate 登録
  - `getNestedClassifiersOf(classSymbol)` / `generateNestedClassLikeDeclaration(...)` — ネストクラス生成
  - `getMembersOfConstructedType(classId)` / `generateFunctions(...)` — `inject()` 等のメンバー生成
- **動作の詳細**:
  - `@Inject` 付きクラスの Factory/Provider クラスを FIR として生成（IR フェーズでボディを補完）
  - constructor inject、assisted inject のパラメータを解析してファクトリを生成

#### DependencyGraphFirGenerator (FIR / K2)

- **継承/実装**: `FirDeclarationGenerationExtension`
- **動作の詳細**:
  - `@DependencyGraph` アノテーション付きインターフェース（または抽象クラス）に対して `$$Impl` 実装クラスを FIR として生成
  - `companion object` と `create()` ファクトリ関数も生成
  - abstract な依存グラフプロバイダーメソッドのシグネチャを生成

#### ContributionHintFirGenerator (FIR / K2)

- **継承/実装**: `FirDeclarationGenerationExtension` (実装は `MetroFirDeclarationGenerationExtension` 経由)
- **動作の詳細**:
  - `@Inject` 付きクラスおよびコントリビューションアノテーション付きクラスに対して hint マーカー関数を FIR フェーズで生成
  - 生成した hint は後の IR フェーズでスコープ検索に使用される

#### GraphFactoryFirSupertypeGenerator / ContributedInterfaceSupertypeGenerator (FIR / K2)

- **継承/実装**: `FirSupertypeGenerationExtension`
- **オーバーライドメソッド**: `computeAdditionalSupertypes(classLikeDeclaration, supertypes, typeResolver)`
- **動作の詳細**:
  - `GraphFactoryFirSupertypeGenerator`: `@DependencyGraph.Factory` にファクトリインターフェースのスーパータイプを追加
  - `ContributedInterfaceSupertypeGenerator`: `@ContributesTo` で贡献されたインターフェースをスコープ付きグラフのスーパータイプとして追加

#### MetroIrGenerationExtension (IR / K2)

- **継承/実装**: `IrGenerationExtension`
- **動作の詳細（フェーズ順）**:
  - `CoreTransformers` で一パスで複数の変換を実行:
    - `ContributionTransformer` — コントリビューション収集
    - `MembersInjectorTransformer` — メンバーインジェクション実装
    - `InjectedClassTransformer` — `@Inject` クラスのファクトリ実装
    - `AssistedFactoryTransformer` — アシストインジェクション
    - `BindingContainerTransformer` — `@BindingContainer` 処理
    - `ContributionHintIrTransformer` — hint 関数のボディ生成
    - `CreateGraphTransformer` — グラフ生成
    - `DefaultBindingMirrorTransformer` — デフォルトバインディング
  - 並列処理: `options.parallelThreads > 0` の場合 `ForkJoinPool` で並列実行
  - `DependencyGraphTransformer` で `@DependencyGraph` 実装を生成
  - Perfetto トレース対応（`options.traceEnabled`）

---

## MoshiX

### 登録エントリポイント

- ファイル: `https://github.com/ZacSweers/MoshiX/blob/main/moshi-ir/moshi-compiler-plugin/src/main/kotlin/dev/zacsweers/moshix/ir/compiler/MoshiComponentRegistrar.kt`
- 登録内容:
  - `IrGenerationExtension.registerExtension(MoshiIrGenerationExtension(...))` — IR Extension のみ
  - FIR Extension は登録なし（K1/IR のみで完結する実装）
  - `KEY_ENABLED` が false の場合は何も登録しない

- `MoshiCommandLineProcessor` — `enabled`, `debug`, `enableSealed`, `generatedAnnotation` オプションを処理

### 各機能の詳細

#### MoshiIrGenerationExtension (IR / K2)

- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**:
  - `generate(moduleFragment, pluginContext)` — `MoshiIrVisitor` でモジュールを変換し、生成したアダプタクラスを deferred で対応ファイルに追加
- **動作の詳細**:
  1. `generatedAnnotationName` が指定された場合、シンボルプロバイダーで該当クラスを検索
  2. `MoshiIrVisitor` で `moduleFragment.transform()` を実行
  3. 変換完了後、`deferred` リストのアダプタクラスを対応する `IrFile` の `declarations` に追加

#### MoshiIrVisitor (IR / K2)

- **継承/実装**: `IrElementTransformerVoidWithContext`
- **オーバーライドメソッド**:
  - `visitClassNew(declaration: IrClass)` — `@JsonClass(generateAdapter = true)` を持つクラスを処理
- **動作の詳細**:
  - `@JsonClass(generateAdapter = true)` アノテーションを持つクラスを検出
  - `generator` フィールドが空の場合: `MoshiAdapterGenerator` でアダプタを生成
  - `generator` フィールドが `"sealed:labelKey"` 形式かつ `enableSealed = true` の場合: `SealedAdapterGenerator` でシールドクラス用アダプタを生成
  - 生成したアダプタクラスを `deferredAddedClasses` リストに追加（後で `IrFile` に追加）

#### MoshiAdapterGenerator (IR / K2)

- **動作の詳細**:
  - `targetType()` でクラスのコンストラクタパラメータ・プロパティを解析し `TargetType` を構築
  - コンストラクタパラメータ順にプロパティをソート
  - `AdapterGenerator.prepare()` で `JsonAdapter<T>` を継承した IrClass を生成
  - `fromJson()` / `toJson()` のボディを IR として生成

#### SealedAdapterGenerator (IR / K2)

- **動作の詳細**:
  - `@JsonClass(generator = "sealed:type")` 付きシールドクラスを処理
  - ラベルキーに基づいてポリモーフィックな JSON アダプタを生成
  - サブタイプを走査し各サブタイプのアダプタを生成

---

## redacted

### 登録エントリポイント

- ファイル: `https://github.com/ZacSweers/redacted-compiler-plugin/blob/main/redacted-compiler-plugin/src/main/kotlin/dev/zacsweers/redacted/compiler/RedactedCompilerPluginRegistrar.kt`
- 登録内容:
  - `FirExtensionRegistrarAdapter.registerExtension(RedactedFirExtensionRegistrar(...))` — FIR Extension
  - `IrGenerationExtension.registerExtension(RedactedIrGenerationExtension(...))` — IR Extension
  - `KEY_ENABLED` が false の場合は何も登録しない

- `RedactedCommandLineProcessor` — `enabled`, `replacementString`, `redactedAnnotations`, `unRedactedAnnotation` オプションを処理

- `RedactedFirExtensionRegistrar` (`FirExtensionRegistrar`) が FIR Extension を登録:
  - `+RedactedFirBuiltIns.getFactory(redactedAnnotations, unRedactedAnnotations)` — セッションコンポーネント
  - `+::FirRedactedCheckers` — FIR チェッカー

### 各機能の詳細

#### RedactedFirBuiltIns (FIR / K2)

- **継承/実装**: `FirExtensionSessionComponent`
- **動作の詳細**:
  - `redactedAnnotations: Set<ClassId>` と `unRedactedAnnotations: Set<ClassId>` をセッションコンポーネントとして保持
  - `FirSession.redactedAnnotations` / `FirSession.unRedactedAnnotations` 拡張プロパティ経由でアクセス可能

#### FirRedactedCheckers / FirRedactedDeclarationChecker (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension` / `FirClassChecker`
- **オーバーライドメソッド**:
  - `check(declaration: FirClass)` — `@Redacted` アノテーションが正しく使用されているかを検証
- **動作の詳細**:
  - クラスレベル・プロパティレベルの `@Redacted`/`@Unredacted` アノテーションを収集
  - カスタム `toString()` 関数がある場合はエラー
  - enum クラス/エントリへの適用はエラー
  - data class / value class 以外への適用はエラー
  - object クラスへの `@Redacted` 適用はエラー（スーパータイプが redacted の場合を除く）
  - クラスとプロパティの両方に `@Redacted` がある場合はエラー
  - `@Unredacted` はスーパータイプが `@Redacted` でない限りエラー
- **診断メッセージ**: `RedactedDiagnostics.REDACTED_ERROR` (Severity.ERROR、メッセージは文字列引数 1 つ)

#### RedactedDiagnostics (FIR / K2)

- **継承/実装**: `KtDiagnosticsContainer`
- **定義**:
  - `REDACTED_ERROR by error1<String>(NAME_IDENTIFIER)` — エラーメッセージを `String` 引数として受け取る診断
  - `RedactedErrorMessages` でレンダリングマップを定義（`"{0}"` と `STRING` renderer）
- **注意**: IDE とコンパイラで `PsiElement` クラスが異なるため、リフレクションでクラスを動的解決する実装が含まれる

#### RedactedIrGenerationExtension (IR / K2)

- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**:
  - `generate(moduleFragment, pluginContext)` — `RedactedIrVisitor` でモジュールを変換
- **動作の詳細**:
  - `replacementString`（デフォルト `"██"`）、`redactedAnnotations`、`unRedactedAnnotations` を `RedactedIrVisitor` に渡す

#### RedactedIrVisitor (IR / K2)

- **継承/実装**: `IrElementTransformerVoidWithContext`
- **オーバーライドメソッド**:
  - `visitFunctionNew(declaration: IrFunction)` — `toString()` 関数を発見した場合に処理
- **動作の詳細**:
  - `Any.toString()` をオーバーライドしている `IrSimpleFunction` のみを対象
  - 親クラスのプライマリコンストラクタパラメータと、`@Redacted`/`@Unredacted` プロパティを収集
  - `@Redacted` プロパティは `replacementString`（例: `"██"`）を返すように、未アノテートプロパティは通常通り `toString()` の文字列連結に含める
  - クラスレベル `@Redacted` の場合は全プロパティを redact
  - スーパークラスが `@Redacted` の場合はそのクラスの挙動を継承し `@Unredacted` で個別に除外可能
  - 配列型プロパティは `contentToString()` / `contentDeepToString()` を使用
