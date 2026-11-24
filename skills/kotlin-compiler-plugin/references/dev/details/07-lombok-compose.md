# lombok / compose 詳細

## lombok

### 登録エントリポイント (K1)

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k1/src/org/jetbrains/kotlin/lombok/LombokResolveExtension.kt
- 登録内容:
  - `SyntheticJavaResolveExtension` を実装する `LombokResolveExtension` が `buildProvider()` で `LombokSyntheticJavaPartsProvider` を返す
  - `LombokPluginConfig(configFile: File?)` でlombok.configファイルのパスを受け取る

### 登録エントリポイント (K2)

- K2 では `FirDeclarationGenerationExtension` / `FirStatusTransformerExtension` 系の Extension を直接登録する形式
  - `LombokConstructorsGenerator` (FirDeclarationGenerationExtension)
  - `BuilderGenerator` / `SuperBuilderGenerator` (AbstractBuilderGenerator → FirDeclarationGenerationExtension)
  - `DeclarationWithValueAnnStatusTransformer` (FirStatusTransformerExtension)

---

### 各機能の詳細

#### LombokResolveExtension (K1)

- **継承/実装**: `SyntheticJavaResolveExtension`
- **オーバーライドメソッド**: `buildProvider(): SyntheticJavaPartsProvider`
- **動作の詳細**:
  - `LombokConfig.parse(configFile)` でlombok.configを解析して `LombokSyntheticJavaPartsProvider` に渡す
  - `buildProvider()` が返すプロバイダが実際のメンバ生成を担う

---

#### LombokSyntheticJavaPartsProvider (K1)

- **継承/実装**: `SyntheticJavaPartsProvider`
- **オーバーライドメソッド**:
  - `getMethodNames(thisDescriptor, c)`
  - `generateMethods(thisDescriptor, name, result, c)`
  - `getStaticFunctionNames(thisDescriptor, c)`
  - `generateStaticFunctions(thisDescriptor, name, result, c)`
  - `generateConstructors(thisDescriptor, result, c)`
  - `getNestedClassNames(thisDescriptor, c)`
  - `generateNestedClass(thisDescriptor, name, result, c)`
  - `modifyField(thisDescriptor, propertyDescriptor, c)`
- **動作の詳細**:
  - 内部に `Processor` リスト (`GetterProcessor`, `SetterProcessor`, `WithProcessor`, `NoArgsConstructorProcessor`, `AllArgsConstructorProcessor`, `RequiredArgsConstructorProcessor`, `BuilderProcessor`) を持つ
  - クラスごとに `SyntheticParts` をキャッシュし、各 Processor の `contribute()` で集積
  - `modifyField` では `ValueFieldModifier` を介して `@Value` アノテーション付きクラスのフィールドを `private` に変更
  - 重複排除ロジック: 引数の数だけで同一シグネチャ判定（Lombokと同仕様）

---

#### Processor インタフェース (K1)

- `interface Processor { fun contribute(classDescriptor, partsBuilder, c) }`
- 各 Processor (`GetterProcessor` など) がこれを実装してメンバ生成ロジックを担う

---

#### GetterProcessor (K1)

- **継承/実装**: `Processor`
- **動作の詳細**:
  - フィールドに `@Getter` / クラスに `@Getter` / `@Data` / `@Value` があれば getter メソッドを生成
  - `@Accessors(fluent=true)` なら `fieldName()` 形式、それ以外は `getXxx()` / boolean型なら `isXxx()` 形式
  - アクセスレベル `NONE` の場合はスキップ

---

#### BuilderProcessor (K1)

- **継承/実装**: `Processor`
- **動作の詳細**:
  - `@Builder` アノテーション付きクラスに対してビルダーパターンのメンバを生成
  - `SyntheticJavaClassDescriptor` (ビルダークラス) をネストクラスとして追加
  - エンティティクラスに `static builder()` メソッドを追加
  - `toBuilder=true` なら `toBuilder()` インスタンスメソッドも追加
  - ビルダークラス内に各フィールドのセッターメソッドと `build()` メソッドを生成
  - `@Singular` フィールドには単数形追加メソッド・複数形追加メソッド・`clearXxx()` の3つを生成
  - Collection型 / Map型 / Guava Table型をサポート、単数形はStringUtilで推測

---

#### ValueFieldModifier (K1)

- **継承/実装**: なし（単純クラス）
- **動作の詳細**:
  - `@Value` アノテーション付きクラスのフィールドで package-private なものを `private` に変更

---

#### annotationConfig.kt (K2)

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/config/annotationConfig.kt
- **動作の詳細**:
  - FIR用のアノテーション設定クラス群 (`ConeLombokAnnotations`) を定義
  - `ConeAnnotationCompanion<T>` / `ConeAnnotationAndConfigCompanion<T>` の基底クラスが lombok.config ファイルとアノテーション引数の両方からコンフィグを統合する
  - `Accessors`, `Getter`, `Setter`, `With`, `NoArgsConstructor`, `AllArgsConstructor`, `RequiredArgsConstructor`, `Data`, `Value`, `Builder`, `SuperBuilder`, `Singular` を全て定義

---

#### DeclarationWithValueAnnStatusTransformer (K2 / FIR)

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/DeclarationWithValueAnnStatusTransformer.kt
- **継承/実装**: `FirStatusTransformerExtension`
- **オーバーライドメソッド**:
  - `needTransformStatus(declaration)`: `FirJavaField` または `FirJavaClass` のみ対象
  - `transformStatus(status, field, containingClass, isLocal)`: `@Value` クラスのpackage-privateフィールドを `private` に変更
  - `transformStatus(status, regularClass, containingClass, isLocal)`: `@Value` クラス自体を `FINAL` modality に変更

---

#### LombokConstructorsGenerator (K2 / FIR)

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/LombokConstructorsGenerator.kt
- **継承/実装**: `FirDeclarationGenerationExtension`
- **オーバーライドメソッド**:
  - `getCallableNamesForClass(classSymbol, context)`: コンストラクタ名・静的ファクトリメソッド名を返す
  - `generateFunctions(callableId, context)`: 静的ファクトリメソッドを生成
  - `generateConstructors(context)`: コンストラクタを生成
- **動作の詳細**:
  - `AllArgsConstructorGeneratorPart`, `NoArgsConstructorGeneratorPart`, `RequiredArgsConstructorGeneratorPart` の3つのパートを統合
  - `FirCache` でクラスごとにコンストラクタ/メソッドをキャッシュ
  - `staticName` が指定されていれば `FirJavaMethod` (静的ファクトリ)、なければ `FirJavaConstructor` を生成

---

#### AbstractConstructorGeneratorPart (K2 / FIR)

- ファイル: https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/AbstractConstructorGeneratorPart.kt
- **継承/実装**: abstract class
- **オーバーライドメソッド** (サブクラスが実装):
  - `getConstructorInfo(classSymbol)`: アノテーション設定を返す
  - `getFieldsForParameters(classSymbol)`: コンストラクタパラメータとなるフィールドを返す
- **動作の詳細**:
  - `staticName` の有無で `FirJavaConstructorBuilder` / `FirJavaMethodBuilder` を使い分け
  - 型パラメータのリマッピング (`JavaTypeSubstitutor`) を実装し、ジェネリッククラス対応
  - `containsExplicitConstructor()` でユーザー定義コンストラクタが存在する場合はスキップ

---

#### AbstractBuilderGenerator / BuilderGenerator / SuperBuilderGenerator (K2 / FIR)

- ファイル:
  - https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/AbstractBuilderGenerator.kt
  - https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/BuilderGenerator.kt
  - https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/SuperBuilderGenerator.kt
- **継承/実装**: `FirDeclarationGenerationExtension`
- **オーバーライドメソッド (AbstractBuilderGenerator)**:
  - `getCallableNamesForClass(classSymbol, context)`: フィールドセッターメソッド名、`builder()` / `toBuilder()` 名を返す
  - `getNestedClassifiersNames(classSymbol, context)`: ビルダークラス名を返す
  - `generateFunctions(callableId, context)`: ビルダーメソッドを生成
  - `generateNestedClassLikeDeclaration(owner, name, context)`: ビルダーネストクラスを生成
- **動作の詳細**:
  - `builderClassesCache` / `builderWithDeclarationsCache` / `functionsCache` の3層キャッシュ構造
  - ビルダークラスが既存Java クラスとして存在する場合は `generateFunctions` でメソッド追加のみ、なければ `FirJavaClass` を新規生成
  - `@Singular` フィールド対応 (Collection / Map / Guava Table): 単数形追加・複数形追加・`clearXxx()` の3メソッドを生成
  - 同名メソッドが既に存在する場合は生成をスキップ (`addIfNonClashing`)
  - `BuilderGenerator.builderModality = FINAL`, `SuperBuilderGenerator.builderModality = ABSTRACT`
- **SuperBuilderGenerator 固有**:
  - ビルダークラスに型パラメータ `C` (エンティティ型) と `B` (ビルダー型) を追加
  - `self()` (protected abstract) と `build()` (public abstract) を追加
  - スーパービルダーの継承チェーンを `superTypeRefs` に設定

---

## compose

### 登録エントリポイント

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposePlugin.kt
- 登録内容:
  - `ComposePluginRegistrar` が `CompilerPluginRegistrar` を継承
  - `supportsK2 = true`
  - `registerCommonExtensions()` で以下を登録 (K1/K2共通):
    - `StorageComponentContainerContributor`: `ComposableCallChecker`, `ComposableDeclarationChecker`, `ComposableTargetChecker`, `ComposableAnnotationChecker` (K1チェッカー)
    - `DiagnosticSuppressor`: `ComposeDiagnosticSuppressor`
    - `TypeResolutionInterceptor`: `ComposeTypeResolutionInterceptorExtension`
    - `DescriptorSerializerPlugin`: `ClassStabilityFieldSerializationPlugin`
    - `FirExtensionRegistrar`: `ComposeFirExtensionRegistrar` (K2)
  - `IrGenerationExtension`: `ComposeIrGenerationExtension` (K1/K2共通IRパス)
  - K1のみ: `AddHiddenFromObjCSerializationPlugin`

---

### FeatureFlag システム

- `enum class FeatureFlag(featureName, default: Boolean)`: 段階的フィーチャーロールアウト機構
  - `StrongSkipping` (default=true): 強化されたスキップ最適化
  - `IntrinsicRemember` (default=true): remember をインスティンクトとして最適化
  - `OptimizeNonSkippingGroups` (default=true): スキップしない関数のグループ削除
  - `PausableComposition` (default=true): Composition の一時停止サポート
- `-FeatureName` プレフィックスで無効化可能
- 旧来の個別オプション (`strongSkipping`, `intrinsicRemember` 等) から `featureFlag` オプションへの移行を促す非推奨警告を出す

---

### 各機能の詳細

#### ComposeFirExtensionRegistrar (K2 / FIR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposeFirExtensions.kt
- **継承/実装**: `FirExtensionRegistrar`
- **登録内容**:
  - `ComposableFunctionTypeKindExtension`: `@Composable` を関数型の kind として登録
  - `ComposeFirCheckersExtension`: FIRチェッカーを登録
  - `ComposableTargetSessionStorage`: セッションストレージ
  - `registerDiagnosticContainers(ComposeErrors)`

---

#### ComposableFunctionTypeKindExtension (K2 / FIR)

- **継承/実装**: `FirFunctionTypeKindExtension`
- **オーバーライドメソッド**: `FunctionTypeKindRegistrar.registerKinds()`
- **動作の詳細**:
  - `ComposableFunction` と `KComposableFunction` をカスタム関数型 kind として登録
  - `ComposableFunction`: `androidx.compose.runtime.internal` パッケージ、`@Composable` アノテーションに対応、インライン可能、リフレクト型でない
  - K2→K1互換のため、安定版になるまでは通常の関数型 + `@Composable` アノテーション形式でシリアライズ (`serializeAsFunctionWithAnnotationUntil`)

---

#### ComposeFirCheckersExtension (K2 / FIR)

- **継承/実装**: `FirAdditionalCheckersExtension`
- **登録チェッカー**:
  - `declarationCheckers.functionCheckers`: `ComposableFunctionChecker`
  - `declarationCheckers.propertyCheckers`: `ComposablePropertyChecker`
  - `typeCheckers.resolvedTypeRefCheckers`: `ComposableAnnotationChecker`
  - `expressionCheckers.functionCallCheckers`: `ComposableFunctionCallChecker`, `ComposableTargetChecker`
  - `expressionCheckers.propertyAccessExpressionCheckers`: `ComposablePropertyAccessExpressionChecker`
  - `expressionCheckers.callableReferenceAccessCheckers`: `ComposablePropertyReferenceChecker`

---

#### ComposableFunctionChecker (K2 / FIR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposableFunctionChecker.kt
- **継承/実装**: `FirFunctionChecker(MppCheckerKind.Common)`
- **オーバーライドメソッド**: `check(declaration: FirFunction)`
- **動作の詳細**:
  - `@Composable` のオーバーライドミスマッチを `FirErrors.CONFLICTING_OVERLOADS` で報告
  - applier スキームの互換性チェック → `ComposeErrors.COMPOSE_APPLIER_DECLARATION_MISMATCH`
  - `expect`/`actual` の `@Composable` ミスマッチ → `ComposeErrors.MISMATCHED_COMPOSABLE_IN_EXPECT_ACTUAL`
  - suspend 関数 + `@Composable` → `ComposeErrors.COMPOSABLE_SUSPEND_FUN`
  - `main` 関数 + `@Composable` → `ComposeErrors.COMPOSABLE_FUN_MAIN`

---

#### ComposeErrors (K2 / FIR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposeErrors.kt
- **継承/実装**: `KtDiagnosticsContainer`
- **定義されている診断**:
  - `COMPOSABLE_INVOCATION` (error): 非Composable関数内でのComposable呼び出し
  - `COMPOSABLE_EXPECTED` (error): Composable呼び出しがある関数に`@Composable`がない
  - `NONREADONLY_CALL_IN_READONLY_COMPOSABLE` (error): readonly Composable内での非readonly呼び出し
  - `CAPTURED_COMPOSABLE_INVOCATION` (error): ラムダキャプチャ内でのComposable呼び出し
  - `ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE` (error): try式内でのComposable呼び出し
  - `ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE` (error): `runCatching` 内でのComposable呼び出し
  - `MISSING_DISALLOW_COMPOSABLE_CALLS_ANNOTATION` (error): アノテーションなしパラメータ問題
  - `COMPOSABLE_SUSPEND_FUN` (error): suspend + @Composable の組み合わせ禁止
  - `COMPOSABLE_FUN_MAIN` (error): @Composable な main 関数禁止
  - `COMPOSABLE_PROPERTY_REFERENCE` (error): Composableプロパティへの参照禁止
  - `COMPOSABLE_PROPERTY_BACKING_FIELD` (error): Composableプロパティのバッキングフィールド禁止
  - `COMPOSABLE_VAR` (error): Composable var プロパティ禁止
  - `COMPOSE_INVALID_DELEGATE` (error): 無効なデリゲート
  - `MISMATCHED_COMPOSABLE_IN_EXPECT_ACTUAL` (error): expect/actual の @Composable ミスマッチ
  - `COMPOSE_APPLIER_CALL_MISMATCH` (warning): Applierの不一致
  - `COMPOSE_APPLIER_PARAMETER_MISMATCH` (warning): Applierパラメータの不一致
  - `COMPOSE_APPLIER_DECLARATION_MISMATCH` (warning): Applier宣言の不一致
  - `COMPOSABLE_INAPPLICABLE_TYPE` (error): 不適切な型への @Composable
  - `OPEN_COMPOSABLE_DEFAULT_PARAMETER_VALUE` (error): open関数のデフォルト引数問題
  - `ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE` (error): abstract関数のデフォルト引数問題
  - `KEY_CALL_WITH_NO_ARGUMENTS` (error): `key()` の引数なし呼び出し禁止

---

#### ComposeIrGenerationExtension (IR / K1・K2共通)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposeIrGenerationExtension.kt
- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細 (実行順序)**:
  1. `VersionChecker`: Compose Runtimeバージョン確認
  2. `ComposableLambdaAnnotator` (K2のみ): K2でのラムダアノテーション付与
  3. `AddHiddenFromObjCLowering` (Nativeのみ): ObjC非公開メンバのローワリング
  4. `ClassStabilityTransformer`: 全クラスの安定性推論 + `@StabilityInferred` アノテーション付与
  5. `LiveLiteralTransformer` (有効時のみ): ライブリテラルの変換
  6. `ComposableFunInterfaceLowering`: Composable fun interface のローワリング
  7. `DurableFunctionKeyTransformer`: 関数キーの生成
  8. `CopyDefaultValuesFromExpectLowering` (K1のみ): expect からデフォルト値コピー
  9. `ComposableVersionOverloadsLowering`: バージョンオーバーロード生成
  10. `ComposableDefaultParamLowering`: virtual関数のデフォルトラッパー生成
  11. `ComposerLambdaMemoization`: ラムダのメモ化 / Composableラムダのラップ
  12. `ComposerParamTransformer`: 全Composable関数に `$composer` パラメータ追加
  13. `ComposableTargetAnnotationsTransformer`: Target Applierアノテーション変換
  14. `ComposerIntrinsicTransformer`: `currentComposer` 参照をローカルパラメータに置換
  15. `ComposableFunctionBodyTransformer`: restartable/skippable ロジック、グループ管理、`$changed`/`$default` ビットマスク計算
  16. `KlibAssignableParamTransformer` (klib対象のみ)
  17. `WrapJsComposableLambdaLowering` (JS/Wasm のみ)
  18. メトリクス/レポート出力

---

#### ClassStabilityTransformer (IR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ClassStabilityTransformer.kt
- **継承/実装**: `AbstractComposeLowering`, `ClassLoweringPass`, `ModuleLoweringPass`
- **動作の詳細**:
  - 各クラスの安定性を推論し `@StabilityInferred(parameters=N)` アノテーションを付与
  - `$$stable` という static final int フィールドを追加してランタイムで参照可能にする
  - `StabilityBits.STABLE(0b000)` / `StabilityBits.UNSTABLE(0b100)` をビットスロットで表現

---

#### ComposerParamTransformer (IR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposerParamTransformer.kt
- **継承/実装**: `AbstractComposeLowering`, `ModuleLoweringPass`
- **動作の詳細**:
  - 全Composable関数のシグネチャに `$composer: Composer` パラメータを末尾付近に追加
  - 関数型 (`@Composable () -> Unit`) の型引数も変換する `ComposableTypeRemapper` を適用
  - inline lambda の事前スキャン (`ComposeInlineLambdaLocator`) で特殊処理が必要なケースを把握

---

#### ComposableFunctionBodyTransformer (IR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposableFunctionBodyTransformer.kt
- **継承/実装**: `AbstractComposeLowering`
- **動作の詳細**:
  - Composable関数の本体を restartable / skippable に変換する最重要パス
  - `$changed` ビットマスク (パラメータ変化追跡): `ParamState` enum で各パラメータの状態を3ビットで表現
    - `Uncertain(0b000)`, `Same(0b001)`, `Different(0b010)`, `Static(0b011)`
  - グループ管理: `startRestartGroup` / `endRestartGroup` の挿入
  - スキップロジック: パラメータが変化していない場合 `skipToGroupEnd()` で早期リターン
  - source information: `sourceInformationEnabled` が true なら文字列でソース位置を埋め込む
  - trace markers: `traceMarkersEnabled` が true なら Android Trace API のマーカーを挿入

---

#### AbstractComposeLowering (IR / 基底クラス)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/AbstractComposeLowering.kt
- **継承/実装**: `IrElementTransformerVoid`
- **役割**:
  - 全 IR Lowering パスの基底クラス
  - `ComposeCompilerKey` (`GeneratedDeclarationKey`) でコンパイラ生成宣言を識別
  - `FeatureFlag.enabled` 拡張プロパティで機能フラグの有効/無効を参照
  - `getTopLevelClass`, `finderForBuiltins` などのユーティリティを提供

---

#### ComposerLambdaMemoization (IR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposerLambdaMemoization.kt
- **継承/実装**: `AbstractComposeLowering`
- **動作の詳細**:
  - 通常のラムダ (`remember { lambda }` パターン) をメモ化
  - Composableラムダを `ComposableLambda` / `ComposableLambdaN` ラッパーでラップ

---

#### ComposableTargetAnnotationsTransformer (IR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposableTargetAnnotationsTransformer.kt
- **継承/実装**: `AbstractComposeLowering`
- **動作の詳細**:
  - `@ComposableTarget` アノテーションを推論・伝播
  - ターゲット Applier の一致チェック

---

#### DurableFunctionKeyTransformer (IR)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/DurableFunctionKeyTransformer.kt
- **継承/実装**: `AbstractComposeLowering`
- **動作の詳細**:
  - 各Composable関数に決定論的な整数キーを付与 (`@FunctionKeyMeta`)
  - Hot Reload / Live Literals で関数を特定するために使用

---

#### AddHiddenFromObjCLowering (IR / Native専用)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/AddHiddenFromObjCLowering.kt
- **継承/実装**: `AbstractComposeLowering`
- **動作の詳細**:
  - コンパイラ生成のComposable関数に `@HiddenFromObjC` アノテーションを付与
  - iOS (Kotlin/Native) から不要なAPIを非公開にする

---

#### WrapJsComposableLambdaLowering (IR / JS・Wasm専用)

- ファイル: https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/WrapJsComposableLambdaLowering.kt
- **継承/実装**: `AbstractComposeLowering`
- **動作の詳細**:
  - JS / Wasm プラットフォーム向けにComposableラムダを追加でラップ

---

## K1 / K2 対応比較表

| 機能 | K1 実装 | K2 実装 |
|------|---------|---------|
| lombok コンストラクタ生成 | `LombokSyntheticJavaPartsProvider` + `AbstractConstructorProcessor` | `LombokConstructorsGenerator` (FirDeclarationGenerationExtension) |
| lombok Builder 生成 | `BuilderProcessor` | `BuilderGenerator` / `SuperBuilderGenerator` (AbstractBuilderGenerator) |
| lombok フィールド修飾 | `ValueFieldModifier` + `modifyField()` | `DeclarationWithValueAnnStatusTransformer` (FirStatusTransformerExtension) |
| Compose 型チェック | `ComposableCallChecker` etc. (StorageComponentContainerContributor) | `ComposeFirCheckersExtension` (FirAdditionalCheckersExtension) |
| Composable 関数型 | `TypeResolutionInterceptor` | `ComposableFunctionTypeKindExtension` (FirFunctionTypeKindExtension) |
| IR 変換 | `ComposeIrGenerationExtension` (共通) | `ComposeIrGenerationExtension` (共通) |
