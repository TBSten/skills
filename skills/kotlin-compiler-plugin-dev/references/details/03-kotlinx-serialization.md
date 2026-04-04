# kotlinx.serialization 詳細

## kotlinx.serialization

### 登録エントリポイント

- ファイル: [SerializationComponentRegistrar.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.cli/src/org/jetbrains/kotlinx/serialization/compiler/extensions/SerializationComponentRegistrar.kt)
- 登録内容:
  - `DescriptorSerializerPlugin.registerExtension(SerializationDescriptorSerializerPlugin())` — K1 メタデータ書き出し
  - `SyntheticResolveExtension.registerExtension(SerializationResolveExtension(...))` — K1 合成メンバ生成
  - `IrGenerationExtension.registerExtension(SerializationLoweringExtension(...))` — IR 変換（K1/K2 共通）
  - `StorageComponentContainerContributor.registerExtension(SerializationPluginComponentContainerContributor())` — K1 宣言チェッカー登録
  - `FirExtensionRegistrar.registerExtension(FirSerializationExtensionRegistrar())` — K2 FIR Extension 登録
- `supportsK2 = true`

### FIR Extension 登録ポイント（K2）

- ファイル: [FirSerializationExtensionRegistrar.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/FirSerializationExtensionRegistrar.kt)
- 登録内容:
  - `SerializationFirResolveExtension` — FIR メンバ生成
  - `SerializationFirSupertypesExtension` — FIR スーパータイプ追加
  - `FirSerializationCheckersComponent` — FIR チェッカー
  - `FirSerializationMetadataSerializerPlugin` — FIR メタデータへの情報書き出し
  - `DependencySerializationInfoProvider` (service) — ランタイム依存情報
  - `FirSerializablePropertiesProvider` (service) — シリアライズ可能プロパティ収集
  - `FirVersionReader` (service) — ランタイムバージョン読み取り
  - `ContextualSerializersProvider` (service) — コンテキストシリアライザー解決
  - `registerDiagnosticContainers(FirSerializationErrors)` — 診断メッセージ登録

---

### 各機能の詳細

#### SerializationFirResolveExtension (FIR / K2)

- **継承/実装**: `FirDeclarationGenerationExtension(session)`
- **オーバーライドメソッド**:
  - `getNestedClassifiersNames()` — `@Serializable` クラスに `Companion` オブジェクトと `$serializer` クラスの名前を返す
  - `generateNestedClassLikeDeclaration()` — `Companion` または `$serializer` クラスの FIR 宣言を生成
  - `getCallableNamesForClass()` — Companion に `serializer()` / `generatedSerializer()`、`$serializer` に `serialize` / `deserialize` / `descriptor` 等を返す
  - `generateFunctions()` — `serializer()`, `serialize()`, `deserialize()`, `childSerializers()`, `typeParamsSerializers()` の FIR 関数を生成
  - `generateProperties()` — `$serializer` の `descriptor` プロパティを生成
  - `generateConstructors()` — `$serializer` のプライベートデフォルトコンストラクタ（型パラメータがある場合は公開コンストラクタも）を生成
  - `FirDeclarationPredicateRegistrar.registerPredicates()` — `@Serializable`, `@MetaSerializable`, `@KeepGeneratedSerializer` の述語を登録
- **動作の詳細**:
  - `@Serializable` クラスにはコンパニオンオブジェクト（存在しない場合のみ）と `$serializer` ネストクラスを FIR フェーズで生成
  - `$serializer` は `GeneratedSerializer<T>` を継承するオブジェクトまたはクラス（型パラメータがある場合）
  - コンパニオンが `SerializerFactory` インタフェースを必要とする場合（非 JVM プラットフォーム）は `generateSerializerFactoryVararg` も生成
  - 生成した関数・プロパティは JS プラットフォームで `@JsExportIgnore` が付く

- ファイル: [SerializationFirResolveExtension.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/SerializationFirResolveExtension.kt)

---

#### SerializationFirSupertypesExtension (FIR / K2)

- **継承/実装**: `FirSupertypeGenerationExtension(session)`
- **オーバーライドメソッド**:
  - `needTransformSupertypes()` — `@Serializer(for=...)` アノテーション付きクラス、または非 JVM での `@Serializable` オブジェクト/Companion の場合に `true`
  - `computeAdditionalSupertypes()` — 追加するスーパータイプを返す
  - `FirDeclarationPredicateRegistrar.registerPredicates()` — `serializerFor` 述語を登録
- **動作の詳細**:
  - `@Serializer(for=SomeClass::class)` が付いたクラスに `KSerializer<SomeClass>` スーパータイプを追加
  - 非 JVM プラットフォームでは `@Serializable` オブジェクトや Companion に `SerializerFactory` スーパータイプを追加（`serializer()` のオーバーロード解決に必要）
  - JVM では `SerializerFactory` は必要ない（オーバーロードアクセス制御に JVM のアクセス制御が使えるため）

- ファイル: [SerializationFirSupertypesExtension.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/SerializationFirSupertypesExtension.kt)

---

#### FirSerializationCheckersComponent (FIR / K2)

- **継承/実装**: `FirAdditionalCheckersExtension(session)`
- **オーバーライドメソッド**:
  - `declarationCheckers` — `classCheckers` に `FirSerializationPluginClassChecker` を登録
  - `expressionCheckers` — `functionCallCheckers` に `FirSerializationPluginCallChecker` を登録
- **動作の詳細**:
  - クラスチェッカーと関数呼び出しチェッカーをまとめて登録するコンテナ

- ファイル: [FirSerializationCheckersComponent.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/checkers/FirSerializationCheckersComponent.kt)

---

#### FirSerializationPluginClassChecker (FIR / K2)

- **継承/実装**: `FirClassChecker(MppCheckerKind.Common)`
- **オーバーライドメソッド**:
  - `check(declaration: FirClass)` — `@Serializable` クラスに対する包括的なチェックを実施
- **動作の詳細**:
  - `checkMetaSerializableApplicable` — `@MetaSerializable` がアノテーションクラス以外に使われていないか確認
  - `checkInheritableSerialInfoNotRepeatable` — `@InheritableSerialInfo` と `@Repeatable` の併用を禁止
  - `checkEnum` — enum クラスのシリアル名重複チェック
  - `checkExternalSerializer` — `@Serializer(for=...)` の外部シリアライザーの有効性チェック
  - `checkKeepGeneratedSerializer` — `@KeepGeneratedSerializer` の使用条件チェック
  - `buildSerializableProperties` → `checkCorrectTransientAnnotationIsUsed` — `@Transient` アノテーション正確性チェック
  - `checkProtobufProperties` — `@ProtoNumber` の重複チェック
  - `checkTransients` — `@Transient` なプロパティに初期値があるか確認
  - `analyzePropertiesSerializers` — 各プロパティのシリアライザーが見つかるか確認
  - `checkInheritedAnnotations` — 継承したシリアル情報アノテーションの整合性チェック
  - `checkVersions` — Kotlin バージョン・ランタイムバージョン要件チェック

- **診断メッセージ**: `FirSerializationErrors` 参照

- ファイル: [FirSerializationPluginClassChecker.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/checkers/FirSerializationPluginClassChecker.kt)

---

#### FirSerializationPluginCallChecker (FIR / K2)

- **継承/実装**: `FirFunctionCallChecker(MppCheckerKind.Common)`
- **オーバーライドメソッド**:
  - `check(expression: FirFunctionCall)` — `Json { }` 関数呼び出しをチェック
- **動作の詳細**:
  - `Json {}` または `Json(Json.Default) {}` のように既定と等価な `Json` フォーマット生成を検出して警告
  - `JSON_FORMAT_REDUNDANT_DEFAULT` — `Json.Default` と同等の引数で `Json` を作成している場合
  - `JSON_FORMAT_REDUNDANT` — 生成した `Json` オブジェクトにすぐに `.encodeToString()` などをチェーンしている場合

- ファイル: [FirSerializationPluginCallChecker.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/checkers/FirSerializationPluginCallChecker.kt)

---

#### FirSerializationMetadataSerializerPlugin (FIR / K2)

- **継承/実装**: `FirMetadataSerializerPlugin(session)` (`@OptIn(FirExtensionApiInternals::class)`)
- **オーバーライドメソッド**:
  - `registerProtoExtensions(symbol, stringTable, protoRegistrar)` — クラスのコンパイル時メタデータに拡張を書き込む
- **動作の詳細**:
  - `open`/`abstract` で `@Serializable` なクラスに対し、プロパティのプログラム順序をメタデータの protobuf 拡張(`propertiesNamesInProgramOrder`)として書き出す
  - これにより、サブクラスがメタデータからシリアライズ順序を読み取ることができる
  - `needSaveProgramOrder` プロパティで対象クラスを絞り込む（`open`/`abstract` のみ対象）

- ファイル: [FirSerializationMetadataSerializerPlugin.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/FirSerializationMetadataSerializerPlugin.kt)

---

#### FirSerializationErrors (FIR 診断定義 / K2)

- **継承/実装**: `KtDiagnosticsContainer`
- **動作の詳細**: K2 で使用するすべての診断エラー/警告を定義する。`KtDefaultErrorMessagesSerialization` でレンダリング

- **診断メッセージ一覧**:

  | 名前 | 重大度 | 概要 |
  |------|--------|------|
  | `INLINE_CLASSES_NOT_SUPPORTED` | ERROR | インラインクラスが未対応のランタイムバージョン |
  | `PLUGIN_IS_NOT_ENABLED` | WARNING | プラグイン未有効化 |
  | `ANONYMOUS_OBJECTS_NOT_SUPPORTED` | ERROR | 匿名オブジェクトは非対応 |
  | `INNER_CLASSES_NOT_SUPPORTED` | ERROR | inner クラスは非対応 |
  | `EXPLICIT_SERIALIZABLE_IS_REQUIRED` | WARNING | `@Serializable` の明示が必要 |
  | `COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED` | ERROR | コンパニオンオブジェクトをカスタムシリアライザーとして使用は非推奨 |
  | `COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS` | ERROR | 別の `@Serializable` クラス内での Companion シリアライザー |
  | `COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS` | WARNING | 非 `@Serializable` クラス内での Companion シリアライザー |
  | `COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS` | ERROR | `@Serializable` クラス内の Companion が `@Serializable` |
  | `SERIALIZABLE_ANNOTATION_IGNORED` | ERROR | `@Serializable` が無視される |
  | `NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR` | ERROR | 非シリアライズ可能な親に引数なしコンストラクタが必要 |
  | `PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY` | ERROR | プライマリコンストラクタのパラメータがプロパティでない |
  | `DUPLICATE_SERIAL_NAME` | ERROR | シリアル名の重複 |
  | `DUPLICATE_SERIAL_NAME_ENUM` | ERROR | enum でのシリアル名重複 |
  | `SERIALIZER_NOT_FOUND` | ERROR | シリアライザーが見つからない |
  | `SERIALIZER_NULLABILITY_INCOMPATIBLE` | ERROR | シリアライザーの null 許容性の不一致 |
  | `SERIALIZER_TYPE_INCOMPATIBLE` | WARNING | シリアライザーの型の不一致 |
  | `ABSTRACT_SERIALIZER_TYPE` | ERROR | シリアライザーが抽象型 |
  | `LOCAL_SERIALIZER_USAGE` | ERROR | ローカルシリアライザーの使用 |
  | `CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT` | ERROR | カスタムシリアライザーのパラメータ数が不正 |
  | `CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE` | ERROR | カスタムシリアライザーのパラメータ型が不正 |
  | `CUSTOM_SERIALIZER_MAY_BE_INACCESSIBLE` | WARNING | カスタムシリアライザーにアクセスできない可能性 |
  | `GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED` | ERROR | ジェネリック配列要素は非対応 |
  | `TRANSIENT_MISSING_INITIALIZER` | ERROR | `@Transient` プロパティに初期値がない |
  | `TRANSIENT_IS_REDUNDANT` | WARNING | `@Transient` が冗長 |
  | `INCORRECT_TRANSIENT` | WARNING | `@kotlin.jvm.Transient` を誤使用（`@kotlinx.serialization.Transient` を使うべき） |
  | `REQUIRED_KOTLIN_TOO_HIGH` | ERROR | 必要 Kotlin バージョンが高すぎる |
  | `PROVIDED_RUNTIME_TOO_LOW` | ERROR | ランタイムバージョンが低すぎる |
  | `INCONSISTENT_INHERITABLE_SERIALINFO` | ERROR | 継承可能シリアル情報の不整合 |
  | `META_SERIALIZABLE_NOT_APPLICABLE` | ERROR | `@MetaSerializable` の不正な使用 |
  | `INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE` | ERROR | `@InheritableSerialInfo` と `@Repeatable` の併用禁止 |
  | `EXTERNAL_SERIALIZER_USELESS` | WARNING | 外部シリアライザーが無意味 |
  | `EXTERNAL_CLASS_NOT_SERIALIZABLE` | ERROR | 外部クラスが `@Serializable` でない |
  | `EXTERNAL_CLASS_IN_ANOTHER_MODULE` | ERROR | 外部クラスが別モジュール |
  | `EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR` | ERROR | 外部シリアライザーに適切なコンストラクタがない |
  | `KEEP_SERIALIZER_ANNOTATION_USELESS` | ERROR | `@KeepGeneratedSerializer` が無意味 |
  | `KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC` | ERROR | ポリモーフィッククラスへの `@KeepGeneratedSerializer` |
  | `PROTOBUF_PROTO_NUM_DUPLICATED` | WARNING | `@ProtoNumber` の重複 |
  | `JSON_FORMAT_REDUNDANT_DEFAULT` | WARNING | `Json.Default` と等価な `Json {}` 生成 |
  | `JSON_FORMAT_REDUNDANT` | WARNING | 冗長な `Json {}` 生成 |

- ファイル: [FirSerializationErrors.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/checkers/FirSerializationErrors.kt)

---

#### FirSerializablePropertiesProvider (FIR Service / K2)

- **継承/実装**: `FirExtensionSessionComponent(session)`
- **動作の詳細**:
  - セッションスコープのキャッシュ付きサービス（`FirCache` を使用）
  - `getSerializablePropertiesForClass()` — クラスのシリアライズ可能プロパティ一覧を返す（キャッシュ付き）
  - 主コンストラクタのプロパティかどうか、`@Transient` が付いているか、可視性などを考慮してプロパティをフィルタリング
  - enum クラスは専用の処理を行う

- ファイル: [FirSerializablePropertiesProvider.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/services/FirSerializablePropertiesProvider.kt)

---

#### DependencySerializationInfoProvider (FIR Service / K2)

- **継承/実装**: `FirExtensionSessionComponent(session)`
- **動作の詳細**:
  - `useGeneratedEnumSerializer` — ランタイムに `enumSerializerFactory` / `annotatedEnumSerializerFactory` が存在するかチェック（古いランタイム対応）
  - `getClassFromSerializationPackage(name)` — `kotlinx.serialization` パッケージからクラスシンボルを取得（キャッシュ付き）
  - `getClassFromInternalSerializationPackage(name)` — `kotlinx.serialization.internal` パッケージからクラスシンボルを取得

- ファイル: [DependencySerializationInfoProvider.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/services/DependencySerializationInfoProvider.kt)

---

#### FirVersionReader (FIR Service / K2)

- **継承/実装**: `FirExtensionSessionComponent(session)`
- **動作の詳細**:
  - `runtimeVersions` — ランタイムの `KSerializer` クラスのソース情報からバージョンを読み取り、`RuntimeVersions` として返す（遅延評価・キャッシュ付き）
  - チェッカーがランタイムバージョン要件（`REQUIRED_KOTLIN_TOO_HIGH` / `PROVIDED_RUNTIME_TOO_LOW`）を確認するために使用

- ファイル: [FirVersionReader.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/services/FirVersionReader.kt)

---

#### ContextualSerializersProvider (FIR Service / K2)

- **継承/実装**: `FirExtensionSessionComponent(session)`
- **動作の詳細**:
  - `getContextualKClassListForFile()` — ファイルの `@Contextual` / `@UseContextualSerialization` アノテーションから型一覧を取得（キャッシュ付き）
  - `additionalSerializersInScopeCache` — `@UseSerializers` で追加されたシリアライザーをマップとしてキャッシュ
  - チェッカーが「シリアライザーが見つからない」エラーを報告する前に、コンテキストシリアライザーで解決できるか確認するために使用

- ファイル: [ContextualSerializersProvider.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/services/ContextualSerializersProvider.kt)

---

#### FirSerializationPredicates (FIR Predicates / K2)

- **動作の詳細**:
  - `serializerFor` — `@Serializer(for=...)` アノテーション付きクラスを特定
  - `hasMetaAnnotation` — `@MetaSerializable` でメタアノテーションされたクラスを特定
  - `annotatedWithSerializableOrMeta` — `@Serializable` または `@MetaSerializable` メタアノテーション付きクラスを特定
  - `annotatedWithKeepSerializer` — `@KeepGeneratedSerializer` 付きクラスを特定

- ファイル: [FirSerializationPredicates.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k2/src/org/jetbrains/kotlinx/serialization/compiler/fir/FirSerializationPredicates.kt)

---

#### SerializationLoweringExtension (IR / K1・K2 共通)

- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**:
  - `generate(moduleFragment, pluginContext)` — 2パスの IR 変換を実行
  - `getPlatformIntrinsicExtension()` — JVM バックエンドで `serializer<T>()` のインライン最適化を提供
- **動作の詳細**:
  - **Pass 1 (`SerializerClassPreLowering`)**: `IrPreGenerator` を使って、後のパスで参照できるよう `write$Self()` メソッドおよびデシリアライゼーションコンストラクタの宣言のみを事前生成（ボディなし）
  - **Pass 2 (`SerializerClassLowering`)**: 以下の IR ジェネレーターを順に実行:
    - `SerializableIrGenerator.generate()` — `@Serializable` クラスのデシリアライゼーションコンストラクタ本体と `write$Self()` 本体を生成
    - `SerializerIrGenerator.generate()` — `$serializer` オブジェクト/クラスの `serialize()` / `deserialize()` / `descriptor` 本体を生成
    - `SerializableCompanionIrGenerator.generate()` — Companion の `serializer()` メソッド本体を生成
    - `SerialInfoImplJvmIrGenerator.generateImplementationFor()` — JVM のみ: `@SerialInfo` アノテーションの実装クラスを生成
  - **Intrinsic 最適化**: JVM かつランタイムに `noCompiledSerializer` 関数が存在する場合、`serializer<T>()` 呼び出しを直接シリアライザー取得に置き換える

- ファイル: [SerializationLoweringExtension.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/extensions/SerializationLoweringExtension.kt)

---

#### SerializableIrGenerator (IR / K1・K2 共通)

- **継承/実装**: `BaseIrGenerator(irClass, compilerContext)`
- **動作の詳細**:
  - `generateInternalConstructor()` — デシリアライゼーション用の内部コンストラクタ本体を生成（各プロパティへの代入・デフォルト値処理）
  - `generateWriteSelf()` — JVM 用の `write$Self()` 静的メソッド本体を生成（継承階層でのシリアライズ委譲）
  - `generate()` 静的メソッド — 上記メソッドを適切な条件でクラスに適用
  - `cachedChildSerializers` — Companion に子シリアライザーのキャッシュプロパティを追加（内部シリアライズ最適化）

- ファイル: [SerializableIrGenerator.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/backend/ir/SerializableIrGenerator.kt)

---

#### SerializerIrGenerator (IR / K1・K2 共通)

- **継承/実装**: `BaseIrGenerator(irClass, compilerContext)`
- **動作の詳細**:
  - `generateSave()` — `serialize(encoder, value)` メソッドの本体を生成。`encoder.beginStructure()` を呼び出し、各プロパティを `encodeXxx()` でエンコード
  - `generateLoad()` — `deserialize(decoder)` メソッドの本体を生成。フィールドインデックスのループ、デフォルト値の処理、ビットマスクによる必須フィールドチェックを含む
  - `generateSerialDesc()` — `descriptor` プロパティの初期化コードを生成
  - 型パラメータがある場合は `childSerializers()` / `typeParamsSerializers()` も生成
  - `generate()` 静的メソッド — クラスが `$serializer` であれば処理を適用

- ファイル: [SerializerIrGenerator.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/backend/ir/SerializerIrGenerator.kt)

---

#### SerializableCompanionIrGenerator (IR / K1・K2 共通)

- **継承/実装**: `BaseIrGenerator(irClass, compilerContext)`
- **動作の詳細**:
  - Companion の `serializer(typeSerial0, typeSerial1, ...)` メソッド本体を生成
  - 型パラメータがない場合はシングルトン `$serializer` を返す
  - 型パラメータがある場合は `$serializer(typeSerial0, ...)` コンストラクタを呼び出す
  - `generateSerializerFactoryVararg()` 相当の実装も含む

- ファイル: [SerializableCompanionIrGenerator.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/backend/ir/SerializableCompanionIrGenerator.kt)

---

#### IrPreGenerator (IR / K2 特有)

- **継承/実装**: `BaseIrGenerator(irClass, compilerContext)`
- **動作の詳細**:
  - `preGenerateWriteSelfMethodIfNeeded()` — JVM のみ: `write$Self()` のシグネチャのみを事前生成（ボディなし）
  - `preGenerateDeserializationConstructorIfNeeded()` — デシリアライゼーションコンストラクタのシグネチャのみを事前生成
  - K2 では FIR フェーズで純粋な合成関数が推奨されないため、IR フェーズで明示的に事前宣言が必要
  - コメント: "Generates only specific declarations, but NOT their bodies. This pass is needed to be able to reference these declarations from other generated bodies"

- ファイル: [IrPreGenerator.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.backend/src/org/jetbrains/kotlinx/serialization/compiler/backend/ir/IrPreGenerator.kt)

---

#### SerializationResolveExtension (Frontend / K1 のみ)

- **継承/実装**: `SyntheticResolveExtension`
- **オーバーライドメソッド**:
  - `getSyntheticNestedClassNames()` — `@Serializable` クラスに `$serializer` ネストクラス名を返す
  - `getPossibleSyntheticNestedClassNames()` — IDE 用: 可能なネストクラス名を返す
  - `getSyntheticFunctionNames()` — Companion に `serializer()` / `generatedSerializer()`、`@Serializable` クラスに JVM のみ `write$Self()` を返す
  - `getSyntheticPropertiesNames()` — `$serializer` に `typeSerial0`, `typeSerial1`, ... プロパティ名を返す
  - `generateSyntheticClasses()` — `$serializer` クラスの ClassDescriptor を生成
  - `getSyntheticCompanionObjectNameIfNeeded()` — Companion がない `@Serializable` クラスに Companion 名を返す
  - `addSyntheticSupertypes()` — `$serializer` に `GeneratedSerializer<T>` スーパータイプを追加
  - `generateSyntheticSecondaryConstructors()` — デシリアライゼーション用セカンダリコンストラクタ descriptor を追加
  - `generateSyntheticMethods()` — シリアライザーメソッドの descriptor を生成（`KSerializerDescriptorResolver` に委譲）
  - `generateSyntheticProperties()` — シリアライザープロパティの descriptor を生成

- ファイル: [SerializationResolveExtension.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k1/src/org/jetbrains/kotlinx/serialization/compiler/extensions/SerializationResolveExtension.kt)

---

#### SerializationDescriptorSerializerPlugin (Frontend / K1 のみ)

- **継承/実装**: `DescriptorSerializerPlugin`
- **オーバーライドメソッド**:
  - `afterClass()` — クラスの protobuf メタデータ書き出し後に呼ばれる
- **動作の詳細**:
  - `open`/`abstract` で `@Serializable` なクラスに対し、プロパティのプログラム順序を protobuf 拡張(`propertiesNamesInProgramOrder`)としてメタデータに書き込む
  - サブクラスが正しい順序でシリアライズするために必要
  - `putIfNeeded()` — `SerializationResolveExtension` 経由で事前にプロパティ情報が格納される
  - IDE では呼ばれない（メモリリーク防止のため）

- ファイル: [SerializationDescriptorSerializerPlugin.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k1/src/org/jetbrains/kotlinx/serialization/compiler/extensions/SerializationDescriptorSerializerPlugin.kt)

---

#### SerializationPluginDeclarationChecker (Frontend / K1 のみ)

- **継承/実装**: `DeclarationChecker`
- **オーバーライドメソッド**:
  - `check(declaration, descriptor, context)` — `ClassDescriptor` に対して包括的なチェックを実施
- **動作の詳細**:
  - `FirSerializationPluginClassChecker` の K1 対応版。同様のチェックロジックを `ClassDescriptor`/`BindingTrace` API で実装
  - `SerializationErrors`（Java）の診断を報告
  - IDE では `VersionReader` の呼び出しをスキップ（パフォーマンス配慮）

- ファイル: [SerializationPluginDeclarationChecker.kt](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k1/src/org/jetbrains/kotlinx/serialization/compiler/diagnostic/SerializationPluginDeclarationChecker.kt)

---

#### SerializationErrors (Frontend 診断定義 / K1 のみ)

- **継承/実装**: Java `interface`（`DiagnosticFactory0/1/2/3` を使用）
- **動作の詳細**: K1 で使用するすべての診断エラー/警告を定義する。K2 の `FirSerializationErrors` に対応するが、Java で書かれており `BindingTrace` API を使用

- ファイル: [SerializationErrors.java](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlinx-serialization/kotlinx-serialization.k1/src/org/jetbrains/kotlinx/serialization/compiler/diagnostic/SerializationErrors.java)

---

## モジュール構成まとめ

| モジュール | 役割 |
|-----------|------|
| `kotlinx-serialization.cli` | `CompilerPluginRegistrar` エントリポイント + CLI オプション |
| `kotlinx-serialization.k2` | K2/FIR 専用の Extension（宣言生成、型追加、チェッカー） |
| `kotlinx-serialization.k1` | K1 専用の Extension（合成 descriptor 生成、メタデータ書き出し、チェッカー） |
| `kotlinx-serialization.backend` | IR 変換（K1/K2 共通）— serialize/deserialize メソッド本体の生成 |
| `kotlinx-serialization.common` | K1/K2 共通の型定義（`ISerializableProperties`, `NamingConventions` 等） |
| `kotlinx-serialization.embeddable` | 埋め込み用ビルド |

## 処理フロー概要

```
ソースコード
    │
    ▼ FIR フェーズ (K2)
SerializationFirResolveExtension    ─── Companion/$serializer クラス・関数を FIR に追加
SerializationFirSupertypesExtension ─── KSerializer<T>/SerializerFactory スーパータイプ追加
FirSerializationCheckersComponent   ─── バリデーション（エラー/警告）
FirSerializationMetadataSerializerPlugin ─ メタデータに順序情報を書き出し
    │
    ▼ IR フェーズ (K1/K2 共通)
SerializationLoweringExtension
  ├── Pass 1: IrPreGenerator        ─── write$Self / deserialize ctor のシグネチャ事前生成
  └── Pass 2: SerializerClassLowering
        ├── SerializableIrGenerator       ─── deserialize ctor / write$Self 本体
        ├── SerializerIrGenerator         ─── serialize / deserialize / descriptor 本体
        ├── SerializableCompanionIrGenerator ─ serializer() 本体
        └── SerialInfoImplJvmIrGenerator  ─── JVM: @SerialInfo 実装クラス生成
```
