# Overview

AI 向けの Kotlin Compiler Plugin 調査結果。各プラグインの機能を Extension Point 粒度で列挙。

各プラグインのソースコードレベルの詳細 (継承クラス、オーバーライドメソッド、具体的な動作、診断メッセージ) は `details/` ディレクトリを参照。

## 凡例

- **FIR**: K2 コンパイラの Frontend (FIR = Frontend IR) フェーズで動作
- **IR**: Backend の IR (Intermediate Representation) フェーズで動作
- **Analysis (K1)**: K1 コンパイラの Frontend (Analysis) フェーズで動作 (FIR 相当)
- **K2**: K2 コンパイラ対応
- **K1**: K1 (旧) コンパイラ対応
- **both**: K1/K2 両方対応

## 一覧テーブル

| plugin 名 | 実装されている機能 | FIR or IR | K2 or K1 | Source code url |
|---|---|---|---|---|
| noarg | FirNoArgPredicateMatcher - アノテーション付きクラスのマッチング (AbstractSimpleClassPredicateMatchingService) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg.k2/src/org/jetbrains/kotlin/noarg/fir/NoArgAnnotationNameProvider.kt |
| noarg | FirNoArgConstructorGenerator - 引数なしコンストラクタの宣言生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg.k2/src/org/jetbrains/kotlin/noarg/fir/FirNoArgConstructorGenerator.kt |
| noarg | FirNoArgCheckers - noarg 関連の診断チェッカー (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg.k2/src/org/jetbrains/kotlin/noarg/fir/FirNoArgCheckersComponent.kt |
| noarg | NoArgConstructorBodyIrGenerationExtension - 引数なしコンストラクタの IR ボディ生成 (IrGenerationExtension, K2 向け) | IR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg.backend/src/org/jetbrains/kotlin/noarg/NoArgConstructorBodyIrGenerationExtension.kt |
| noarg | NoArgFullConstructorIrGenerationExtension - 引数なしコンストラクタの IR 完全生成 (IrGenerationExtension, K1 向け) | IR | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg.backend/src/org/jetbrains/kotlin/noarg/NoArgFullConstructorIrGenerationExtension.kt |
| noarg | CliNoArgDeclarationChecker - K1 向け宣言チェッカー (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg.k1/src/org/jetbrains/kotlin/noarg/diagnostic/CliNoArgDeclarationChecker.kt |
| allopen | FirAllOpenPredicateMatcher - アノテーション付きクラスのマッチング (AbstractSimpleClassPredicateMatchingService) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen.k2/src/org/jetbrains/kotlin/allopen/fir/FirAllOpenStatusTransformer.kt |
| allopen | FirAllOpenStatusTransformer - クラス/メンバを open 化 (FirStatusTransformerExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen.k2/src/org/jetbrains/kotlin/allopen/fir/FirAllOpenStatusTransformer.kt |
| allopen | CliAllOpenDeclarationAttributeAltererExtension - K1 向け open 化 (DeclarationAttributeAltererExtension) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen.k1/src/org/jetbrains/kotlin/allopen/AllOpenDeclarationAttributeAltererExtension.kt |
| kotlinx-serialization | SerializationFirResolveExtension - $serializer クラス、Companion object、serializer() 関数等の合成宣言を生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/SerializationFirResolveExtension.kt |
| kotlinx-serialization | SerializationFirSupertypesExtension - $serializer に GeneratedSerializer<T> 等のスーパータイプを追加 (FirSupertypeGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/SerializationFirSupertypesExtension.kt |
| kotlinx-serialization | FirSerializationCheckersComponent - 30 種以上の診断チェック (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/checkers/FirSerializationCheckersComponent.kt |
| kotlinx-serialization | FirSerializationMetadataSerializerPlugin - プロパティ順序をメタデータに保存 (FirMetadataSerializerPlugin) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/FirSerializationMetadataSerializerPlugin.kt |
| kotlinx-serialization | SerializationLoweringExtension - シリアライザクラスの IR コード生成 (IrGenerationExtension) | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/extensions/SerializationLoweringExtension.kt |
| kotlinx-serialization | SerializationJvmIrIntrinsicSupport - JVM で serializer<T>() をコンパイル時に最適化 (IrIntrinsicExtension) | IR (JVM) | both | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/backend/ir/SerializationJvmIrIntrinsicSupport.kt |
| kotlinx-serialization | SerializationResolveExtension - K1 向けシンセティックメンバ解決 (SyntheticResolveExtension) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k1/src/org/jetbrains/kotlinx/serialization/compiler/extensions/SerializationResolveExtension.kt |
| kotlinx-serialization | SerializationDescriptorSerializerPlugin - K1 向けディスクリプタシリアライズ (DescriptorSerializerPlugin) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k1/src/org/jetbrains/kotlinx/serialization/compiler/extensions/SerializationDescriptorSerializerPlugin.kt |
| kotlinx-serialization | SerializationPluginDeclarationChecker - K1 向け診断チェッカー (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k1/src/org/jetbrains/kotlinx/serialization/compiler/diagnostic/SerializationPluginDeclarationChecker.kt |
| parcelize | FirParcelizeDeclarationGenerator - Parcelable メンバの宣言生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/FirParcelizeDeclarationGenerator.kt |
| parcelize | FirParcelizeCheckersExtension - Parcelize 診断チェッカー (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k2/src/org/jetbrains/kotlin/parcelize/fir/FirParcelizeCheckersExtension.kt |
| parcelize | ParcelizeFirIrGeneratorExtension - K2 向け Parcelable 実装の IR コード生成 (IrGenerationExtension) | IR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.backend/src/org/jetbrains/kotlin/parcelize/ParcelizeFirIrGeneratorExtension.kt |
| parcelize | ParcelizeIrGeneratorExtension - K1 向け Parcelable 実装の IR コード生成 (IrGenerationExtension) | IR | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.backend/src/org/jetbrains/kotlin/parcelize/ParcelizeIrGeneratorExtension.kt |
| parcelize | ParcelizeResolveExtension - K1 向けシンセティックメンバ解決 (SyntheticResolveExtension) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k1/src/org/jetbrains/kotlin/parcelize/ParcelizeResolveExtension.kt |
| parcelize | ParcelizeDeclarationChecker - K1 向け宣言チェッカー (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/parcelize/parcelize-compiler/parcelize.k1/src/org/jetbrains/kotlin/parcelize/ParcelizeDeclarationChecker.kt |
| power-assert | PowerAssertCheckersExtension - power-assert 使用箇所の診断チェッカー (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.frontend/src/org/jetbrains/kotlin/powerassert/PowerAssertCheckersExtension.kt |
| power-assert | PowerAssertIrGenerationExtension - assert 式をメッセージ付きに変換する IR 生成 (IrGenerationExtension) | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/power-assert/power-assert-compiler/power-assert.backend/src/org/jetbrains/kotlin/powerassert/PowerAssertIrGenerationExtension.kt |
| sam-with-receiver | FirSamWithReceiverConventionTransformer - SAM インターフェースの第1パラメータを receiver に変換 (FirSamConversionTransformerExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/sam-with-receiver/sam-with-receiver.k2/src/org/jetbrains/kotlin/samWithReceiver/k2/FirSamWithReceiverConventionTransformer.kt |
| sam-with-receiver | SamWithReceiverResolverExtension - K1 向け SAM 変換の receiver 化 (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/sam-with-receiver/sam-with-receiver.k1/src/org/jetbrains/kotlin/samWithReceiver/SamWithReceiverResolverExtension.kt |
| lombok | LombokService - Lombok 設定ファイルのパースとサービス提供 (FirSessionComponent) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/config/LombokService.kt |
| lombok | AccessorGenerator - @Getter/@Setter のアクセサ生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/AccessorGenerator.kt |
| lombok | WithGenerator - @With の with メソッド生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/WithGenerator.kt |
| lombok | LombokConstructorsGenerator - @NoArgsConstructor/@AllArgsConstructor 等のコンストラクタ生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/LombokConstructorsGenerator.kt |
| lombok | BuilderGenerator - @Builder のビルダークラス・メソッド生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/BuilderGenerator.kt |
| lombok | SuperBuilderGenerator - @SuperBuilder のスーパービルダー生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/SuperBuilderGenerator.kt |
| lombok | DeclarationWithValueAnnStatusTransformer - @Value アノテーション付きクラスのステータス変更 (FirStatusTransformerExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/DeclarationWithValueAnnStatusTransformer.kt |
| lombok | LombokResolveExtension - K1 向け Lombok シンセティック解決 (SyntheticJavaResolveExtension) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/lombok/lombok.k1/src/org/jetbrains/kotlin/lombok/LombokResolveExtension.kt |
| kapt | FirKaptAnalysisHandlerExtension - アノテーション処理の FIR 解析ハンドラ (FirAnalysisHandlerExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kapt/kapt-compiler/src/org/jetbrains/kotlin/kapt/FirKaptAnalysisHandlerExtension.kt |
| compose | ComposableFunctionTypeKindExtension - @Composable 関数型の登録 (FirFunctionTypeKindExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k2/ComposeFirExtensions.kt |
| compose | ComposeFirCheckersExtension - Composable 関連の診断チェッカー群 (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k2/ComposeFirExtensions.kt |
| compose | ComposableCallChecker - K1 向け @Composable 呼び出しチェック (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k1/ComposableCallChecker.kt |
| compose | ComposableDeclarationChecker - K1 向け @Composable 宣言チェック (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k1/ComposableDeclarationChecker.kt |
| compose | ComposableTargetChecker - K1 向け @ComposableTarget 呼び出しチェック (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k1/ComposableTargetChecker.kt |
| compose | ComposableAnnotationChecker - K1 向け @Composable アノテーションチェック (StorageComponentContainerContributor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k1/ComposableAnnotationChecker.kt |
| compose | ComposeDiagnosticSuppressor - Compose 関連の診断抑制 (DiagnosticSuppressor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k1/ComposeDiagnosticSuppressor.kt |
| compose | ComposeTypeResolutionInterceptorExtension - Compose 向け型解決インターセプト (TypeResolutionInterceptor) | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/k1/ComposeTypeResolutionInterceptorExtension.kt |
| compose | ClassStabilityFieldSerializationPlugin - Stability 情報のメタデータシリアライズ (DescriptorSerializerPlugin) | Analysis (K1) | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ClassStabilityFieldSerializationPlugin.kt |
| compose | ComposeIrGenerationExtension - Compose IR トランスフォーム全体 (IrGenerationExtension) | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/ComposeIrGenerationExtension.kt |
| compose | ┗ ClassStabilityTransformer - クラスの安定性フィールド付与 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ClassStabilityTransformer.kt |
| compose | ┗ ComposableFunInterfaceLowering - Composable fun interface の変換 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposableFunInterfaceLowering.kt |
| compose | ┗ DurableFunctionKeyTransformer - 関数キーの生成 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/DurableFunctionKeyTransformer.kt |
| compose | ┗ ComposableDefaultParamLowering - デフォルトパラメータラッパー生成 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposableDefaultParamLowering.kt |
| compose | ┗ ComposerLambdaMemoization - ラムダのメモ化と Composable ラムダのラップ | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposerLambdaMemoization.kt |
| compose | ┗ ComposerParamTransformer - Composable 関数への Composer パラメータ追加 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposerParamTransformer.kt |
| compose | ┗ ComposableTargetAnnotationsTransformer - @ComposableTarget アノテーション推論・付与 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposableTargetAnnotationsTransformer.kt |
| compose | ┗ ComposerIntrinsicTransformer - currentComposer をローカルパラメータに変換 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposerIntrinsicTransformer.kt |
| compose | ┗ ComposableFunctionBodyTransformer - Composable 関数ボディのグループ・スキップ処理挿入 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/ComposableFunctionBodyTransformer.kt |
| compose | ┗ LiveLiteralTransformer - Live Literal コード生成 | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/compose/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/LiveLiteralTransformer.kt |
| js-plain-objects | FirJsPlainObjectsCheckersComponent - @JsPlainObject のバリデーション (external interface のみ許可、メソッド禁止、スーパータイプ制約等 5種の診断) (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/checkers/FirJsPlainObjectsCheckersComponent.kt |
| js-plain-objects | JsPlainObjectsFunctionsGenerator - companion object、invoke ファクトリ関数、copy 関数の合成生成 (FirDeclarationGenerationExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/JsPlainObjectsFunctionsGenerator.kt |
| js-plain-objects | JsPlainObjectsPropertiesProvider - @JsPlainObject 付きインターフェースのプロパティ情報をキャッシュ・提供 (FirSessionComponent) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.k2/src/org/jetbrains/kotlinx/jso/compiler/fir/services/JsPlainObjectsPropertiesProvider.kt |
| js-plain-objects | JsObjectLoweringExtension - invoke/copy の inline 関数本体をトップレベル関数に移動し js("...") 呼び出しコードを生成 (IrGenerationExtension) | IR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/js-plain-objects/compiler-plugin/js-plain-objects.backend/src/org/jetbrains/kotlinx/jso/compiler/backend/JsObjectLoweringExtension.kt |
| KSP | ComponentRegistrar + AnalysisHandlerExtension (K1 でシンボル処理を実行、複数ラウンド対応) | Analysis (K1) | K1 | https://github.com/google/ksp/blob/1.9.25-1.0.20/compiler-plugin/src/main/kotlin/com/google/devtools/ksp/KotlinSymbolProcessingExtension.kt |
| KSP | ResolverImpl (K1 シンボル解決エンジン: アノテーション収集、型解決、オーバーライド解決) | Analysis (K1) | K1 | https://github.com/google/ksp/blob/1.9.25-1.0.20/compiler-plugin/src/main/kotlin/com/google/devtools/ksp/processing/impl/ResolverImpl.kt |
| KSP | IncrementalContext (K1 インクリメンタルビルドの変更追跡) | Analysis (K1) | K1 | https://github.com/google/ksp/blob/1.9.25-1.0.20/compiler-plugin/src/main/kotlin/com/google/devtools/ksp/IncrementalContext.kt |
| KSP | KSP2 スタンドアロン処理エンジン (Analysis API ベース、CompilerPluginRegistrar 不使用、Gradle タスクとして実行) | Analysis API | K2 | https://github.com/google/ksp/blob/main/kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/KotlinSymbolProcessing.kt |
| KSP | KSP2 ResolverAAImpl (Analysis API ベースのシンボル解決) | Analysis API | K2 | https://github.com/google/ksp/blob/main/kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/ResolverAAImpl.kt |
| atomicfu | AtomicfuPropertyChecker - atomicfu プロパティの可視性チェック (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/diagnostic/AtomicfuPropertyChecker.kt |
| atomicfu | AtomicfuAtomicRefToPrimitiveCallChecker - AtomicRef にプリミティブ型を渡す場合の警告 (FirAdditionalCheckersExtension) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/diagnostic/AtomicfuAtomicRefToPrimitiveCallChecker.kt |
| atomicfu | AtomicfuLoweringExtension - プラットフォーム別 IR 変換のディスパッチ (IrGenerationExtension) | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/extensions/AtomicfuLoweringExtension.kt |
| atomicfu | AtomicfuJvmIrTransformer - JVM 向け atomic プロパティ変換 | IR (JVM) | both | https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/backend/jvm/AtomicfuJvmIrTransformer.kt |
| atomicfu | AtomicfuJsIrTransformer - JS 向け atomic プロパティ変換 | IR (JS) | both | https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/backend/js/AtomicfuJsIrTransformer.kt |
| atomicfu | AtomicfuNativeIrTransformer - Native 向け atomic プロパティ変換 | IR (Native) | both | https://github.com/JetBrains/kotlin/blob/master/plugins/atomicfu/atomicfu-compiler/src/org/jetbrains/kotlinx/atomicfu/compiler/backend/native/AtomicfuNativeIrTransformer.kt |
| koin | FirDeclarationGenerationExtension - モジュール拡張関数の合成生成 | FIR | K2 | https://github.com/InsertKoinIO/koin-compiler-plugin/tree/main/koin-compiler-plugin |
| koin | FirAdditionalCheckersExtension - IC 用 lookup 記録 | FIR | K2 | https://github.com/InsertKoinIO/koin-compiler-plugin/tree/main/koin-compiler-plugin |
| koin | IrGenerationExtension - DSL 変換 (single<T>() → コンストラクタ注入)、アノテーション処理、startKoin 変換、コンパイル時安全性バリデーション、@Monitor ラッピング等の多段 IR 変換 | IR | K2 | https://github.com/InsertKoinIO/koin-compiler-plugin/tree/main/koin-compiler-plugin |
| anvil | AnalysisHandlerExtension - コード生成 (@ContributesTo 等から Dagger モジュールを生成し analysis を再起動) | Analysis (K1) | K1 (supportsK2=true フラグあり) | https://github.com/square/anvil/tree/main/compiler |
| anvil | IrGenerationExtension - @MergeComponent のコントリビューションマージ | IR | K1 (supportsK2=true フラグあり) | https://github.com/square/anvil/tree/main/compiler |
| metro | FirDeclarationGenerationExtension - 7 種の generator を composite でまとめて登録 (DI 関連宣言生成) | FIR | K2 | https://github.com/ZacSweers/metro/tree/main/compiler |
| metro | FirSupertypeGenerationExtension - 2 種のスーパータイプ生成 | FIR | K2 | https://github.com/ZacSweers/metro/tree/main/compiler |
| metro | FirAdditionalCheckersExtension - DI 関連のチェッカー | FIR | K2 | https://github.com/ZacSweers/metro/tree/main/compiler |
| metro | FirStatusTransformerExtension - ステータス変更 | FIR | K2 | https://github.com/ZacSweers/metro/tree/main/compiler |
| metro | FirExtensionSessionComponent - セッションコンポーネント | FIR | K2 | https://github.com/ZacSweers/metro/tree/main/compiler |
| metro | IrGenerationExtension - 8 種の Transformer で DI グラフの実装を生成 | IR | K2 | https://github.com/ZacSweers/metro/tree/main/compiler |
| moshi-ir | IrGenerationExtension - @JsonClass 付きクラスに JsonAdapter を IR で合成生成 (sealed class アダプターも対応) | IR | K2 | https://github.com/ZacSweers/MoshiX/tree/main/moshi-ir |
| redacted | FirAdditionalCheckersExtension - @Redacted の使用バリデーション | FIR | K2 | https://github.com/ZacSweers/redacted-compiler-plugin/tree/main/redacted-compiler-plugin |
| redacted | FirExtensionSessionComponent - セッション情報管理 | FIR | K2 | https://github.com/ZacSweers/redacted-compiler-plugin/tree/main/redacted-compiler-plugin |
| redacted | IrGenerationExtension - toString() メソッドのリライト | IR | K2 | https://github.com/ZacSweers/redacted-compiler-plugin/tree/main/redacted-compiler-plugin |
| debuglog | IrGenerationExtension + DebugLogTransformer - 関数のエントリ/イグジットにログを挿入 | IR | K1 (ComponentRegistrar) | https://github.com/bnorm/debuglog (BraisGabin/DebugLog は存在しない。bnorm/debuglog が正) |
| zipline | IrGenerationExtension - AdapterGenerator, AddAdapterArgumentRewriter, CallAdapterConstructorRewriter 等のブリッジコード生成 | IR | K2 | https://github.com/cashapp/zipline/tree/trunk/zipline-kotlin-plugin |
| arrow-optics | FirDeclarationGenerationExtension - companion object 生成 (Optics コード生成) | FIR | K2 | https://github.com/arrow-kt/arrow/tree/main/arrow-libs/optics/arrow-optics-compiler-plugin |
| back-in-time | FirSupertypeGenerationExtension - スーパータイプの追加 | FIR | K2 | https://github.com/kitakkun/back-in-time-plugin/tree/master/compiler |
| back-in-time | FirDeclarationGenerationExtension - メンバの宣言生成 | FIR | K2 | https://github.com/kitakkun/back-in-time-plugin/tree/master/compiler |
| back-in-time | FirAdditionalCheckersExtension - チェッカー | FIR | K2 | https://github.com/kitakkun/back-in-time-plugin/tree/master/compiler |
| back-in-time | IrGenerationExtension - 5 種の Transformer で状態記録・復元のための IR 変換 | IR | K2 | https://github.com/kitakkun/back-in-time-plugin/tree/master/compiler |
| kondition | FirAdditionalCheckersExtension - 4 種のチェッカー (バリデーションアノテーションの使用チェック) | FIR | K2 | https://github.com/kitakkun/Kondition/tree/master/compiler |
| kondition | IrGenerationExtension - 2 種の Transformer (バリデーションコードの挿入) | IR | K2 | https://github.com/kitakkun/Kondition/tree/master/compiler |
| suspend-kontext | FirAdditionalCheckersExtension - 3 種のチェッカー (suspend 関数のコンテキスト使用チェック) | FIR | K2 | https://github.com/kitakkun/suspend-kontext/tree/master/compiler |
| suspend-kontext | IrGenerationExtension - 1 種の Transformer (コンテキスト注入) | IR | K2 | https://github.com/kitakkun/suspend-kontext/tree/master/compiler |
| aspectk | FirAdditionalCheckersExtension - 2 種のチェッカー (アスペクト定義の妥当性チェック) | FIR | K2 | https://github.com/kitakkun/AspectK/tree/master/compiler |
| aspectk | IrGenerationExtension - 3 種の Transformer (AOP ウィービング) | IR | K2 | https://github.com/kitakkun/AspectK/tree/master/compiler |
| assign-plugin | FirAssignExpressionAltererExtension - val プロパティへの = 代入を assign() メソッド呼び出しに変換 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/assign-plugin/assign-plugin.k2/src/org/jetbrains/kotlin/assignment/plugin/k2/FirAssignmentPluginAssignAltererExtension.kt |
| assign-plugin | FirAdditionalCheckersExtension - assign メソッドの戻り値 Unit チェック、解決エラー報告 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/assign-plugin/assign-plugin.k2/src/org/jetbrains/kotlin/assignment/plugin/k2/FirAssignmentPluginCheckersExtension.kt |
| assign-plugin | FirExtensionSessionComponent - アノテーション照合サービス (スーパータイプ再帰判定) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/assign-plugin/assign-plugin.k2/src/org/jetbrains/kotlin/assignment/plugin/k2/FirAssignAnnotationMatchingService.kt |
| assign-plugin | AssignResolutionAltererExtension - K1 向け代入式変換 | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/assign-plugin/assign-plugin.k1/src/org/jetbrains/kotlin/assignment/plugin/k1/ValueContainerAssignResolutionAltererExtension.kt |
| jvm-abi-gen | ClassGeneratorExtension - コンパイル中の全クラスの ABI 情報 (可視性・インライン性) を収集 | IR (backend) | both | https://github.com/JetBrains/kotlin/blob/master/plugins/jvm-abi-gen/src/org/jetbrains/kotlin/jvm/abi/JvmAbiClassBuilderInterceptor.kt |
| jvm-abi-gen | ClassFileFactoryFinalizerExtension - ABI JAR を出力 (private メンバー除去、非 inline メソッドボディを throw null に置換、Kotlin Metadata ストリッピング) | IR (backend) | both | https://github.com/JetBrains/kotlin/blob/master/plugins/jvm-abi-gen/src/org/jetbrains/kotlin/jvm/abi/JvmAbiOutputExtension.kt |
| kotlin-dataframe | FirFunctionCallRefinementExtension - DataFrame API 呼び出しの戻り型をコンパイル時スキーマ推論で精製 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.k2/src/org/jetbrains/kotlinx/dataframe/plugin/extensions/FunctionCallTransformer.kt |
| kotlin-dataframe | FirExpressionResolutionExtension - DataFrame/DataRow 型の暗黙的拡張レシーバ注入 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.k2/src/org/jetbrains/kotlinx/dataframe/plugin/extensions/ReturnTypeBasedReceiverInjector.kt |
| kotlin-dataframe | FirDeclarationGenerationExtension - @DataSchema クラスのプロパティからトップレベル拡張プロパティ生成 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.k2/src/org/jetbrains/kotlinx/dataframe/plugin/extensions/TopLevelExtensionsGenerator.kt |
| kotlin-dataframe | FirDeclarationGenerationExtension - ローカルスキーマ/スコープクラスのメンバー生成 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.k2/src/org/jetbrains/kotlinx/dataframe/plugin/extensions/TokenContentGenerator.kt |
| kotlin-dataframe | FirSupertypeGenerationExtension - @DataSchema クラスに DataRowSchema スーパータイプ追加 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.k2/src/org/jetbrains/kotlinx/dataframe/plugin/extensions/DataRowSchemaSupertype.kt |
| kotlin-dataframe | FirAdditionalCheckersExtension - cast 検証、inline 内 DataFrame 使用警告、@DataSchema 宣言チェック等 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.k2/src/org/jetbrains/kotlinx/dataframe/plugin/extensions/ExpressionAnalysisAdditionalChecker.kt |
| kotlin-dataframe | IrGenerationExtension - FIR 生成プロパティの getter ボディ生成 (カラムアクセスコード) | IR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-dataframe/kotlin-dataframe.backend/src/org/jetbrains/kotlinx/dataframe/plugin/extensions/IrBodyFiller.kt |
| scripting | FirScriptConfiguratorExtension - スクリプト定義に基づく FIR ビルド設定 (暗黙レシーバ、パラメータ、結果プロパティ等) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/services/FirScriptConfigurationExtensionImpl.kt |
| scripting | FirExtensionSessionComponent - スクリプト定義プロバイダサービス | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/services/FirScriptDefinitionProviderService.kt |
| scripting | FirSamConversionTransformerExtension - スクリプト定義の SAM with Receiver 変換 | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/FirScriptingSamWithReceiverExtensionRegistrar.kt |
| scripting | CollectAdditionalSourceFilesExtension - インポートスクリプトの収集 (再帰解決、トポロジカルソート) | FIR | K2 | https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/fir/CollectAdditionalScriptSourcesExtension.kt |
| scripting | IrGenerationExtension - ScriptsToClassesLowering (スクリプト IR をクラスベース IR に変換) | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/extensions/ScriptLoweringExtension.kt |
| scripting | IrGenerationExtension - ReplSnippetsToClassesLowering (REPL スニペットのクラス変換) | IR | both | https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/extensions/ReplLoweringExtension.kt |
| scripting | SyntheticResolveExtension - K1 スクリプトプロパティ解決 | Analysis (K1) | K1 | https://github.com/JetBrains/kotlin/blob/master/plugins/scripting/scripting-compiler/src/org/jetbrains/kotlin/scripting/compiler/plugin/extensions/ScriptingResolveExtension.kt |

## 補足

### コンパイラプラグインではないもの (調査の結果判明)

| 名前 | 実態 |
|---|---|
| no-copy (KopyKat) | KSP プロセッサであり、Kotlin Compiler Plugin ではない |
| Molecule | Compose ランタイムライブラリのみ。Compiler Plugin を含まない |
| Arrow Analysis (arrow-libs/core) | core にはコンパイラプラグインなし。arrow-optics-compiler-plugin が Optics 用プラグイン |

### K2 Extension Point まとめ (よく使われるもの)

| Extension Point | 用途 | 使用プラグイン数 |
|---|---|---|
| `FirAdditionalCheckersExtension` | バリデーション・診断 | serialization, parcelize, power-assert, atomicfu, compose, redacted, metro, kondition, suspend-kontext, aspectk, back-in-time, koin, noarg, js-plain-objects, assign-plugin, kotlin-dataframe |
| `FirDeclarationGenerationExtension` | メンバ・クラスの合成宣言生成 | serialization, parcelize, noarg, lombok, metro, koin, arrow-optics, back-in-time, js-plain-objects, kotlin-dataframe |
| `IrGenerationExtension` | IR コード本体の生成・変換 | ほぼ全て |
| `FirStatusTransformerExtension` | クラス/メンバのステータス変更 (open 化等) | allopen, lombok, metro |
| `FirSupertypeGenerationExtension` | スーパータイプの追加 | serialization, metro, back-in-time, kotlin-dataframe |
| `FirExtensionSessionComponent` | セッション固有のサービス/状態 | redacted, metro, lombok, js-plain-objects, assign-plugin, compose, scripting |
| `FirSamConversionTransformerExtension` | SAM 変換のカスタマイズ | sam-with-receiver, scripting |
| `FirFunctionTypeKindExtension` | カスタム関数型の登録 | compose |
| `FirMetadataSerializerPlugin` | カスタムメタデータの保存 | serialization |
| `FirAssignExpressionAltererExtension` | 代入式の変換 | assign-plugin |
| `FirFunctionCallRefinementExtension` | 関数呼び出しの戻り型精製 | kotlin-dataframe |
| `FirExpressionResolutionExtension` | 暗黙的レシーバ注入 | kotlin-dataframe |
| `FirScriptConfiguratorExtension` | スクリプト FIR ビルド設定 | scripting |
| `FirAnalysisHandlerExtension` | Analysis フェーズへの完全介入 | kapt |
| `ClassGeneratorExtension` | JVM バックエンドのクラス生成介入 | jvm-abi-gen |
| `ClassFileFactoryFinalizerExtension` | コンパイル完了後のクラスファイル後処理 | jvm-abi-gen |
