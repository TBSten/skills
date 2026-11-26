# noarg / allopen / sam-with-receiver 詳細

> 調査対象ソース: Kotlin 公式リポジトリ (JetBrains/kotlin) に含まれる3プラグインの実装を直接読み込んで記述。

---

## noarg

### 登録エントリポイント

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-cli/src/NoArgPlugin.kt`
- クラス: `NoArgComponentRegistrar` (`CompilerPluginRegistrar`)
- `supportsK2 = true`
- 登録内容:
  - `StorageComponentContainerContributor` → `CliNoArgComponentContainerContributor` (JVM プラットフォーム限定で `CliNoArgDeclarationChecker` を登録)
  - `FirExtensionRegistrar` → `FirNoArgExtensionRegistrar`
  - `IrGenerationExtension` → K2 の場合は `NoArgConstructorBodyIrGenerationExtension`、K1 の場合は `NoArgFullConstructorIrGenerationExtension`

### コマンドラインオプション (`NoArgCommandLineProcessor`)

| オプション名 | 型 | 説明 |
|---|---|---|
| `annotation` | `List<String>` | NoArg を適用するアノテーション FQN |
| `preset` | `List<String>` | プリセット名（現在は `jpa` のみ） |
| `invokeInitializers` | `Boolean` | 生成コンストラクタでインスタンス初期化子を実行するか |

**jpa プリセット**: `javax.persistence.Entity`, `javax.persistence.Embeddable`, `javax.persistence.MappedSuperclass`, `jakarta.persistence.*` の6アノテーション。

### 各機能の詳細

#### `FirNoArgExtensionRegistrar` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-k2/src/FirNoArgExtensionRegistrar.kt`
- **継承/実装**: `FirExtensionRegistrar`
- **オーバーライドメソッド**: `ExtensionRegistrarContext.configurePlugin()`
- **登録内容**:
  - `FirNoArgPredicateMatcher.getFactory(...)` — アノテーション照合サービス
  - `::FirNoArgConstructorGenerator` — コンストラクタ生成
  - `::FirNoArgCheckers` — 診断チェッカー
  - `registerDiagnosticContainers(KtErrorsNoArg)` — 診断コンテナ登録

#### `FirNoArgPredicateMatcher` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-k2/src/FirNoArgPredicateMatcher.kt`
- **継承/実装**: `AbstractSimpleClassPredicateMatchingService`
- **動作の詳細**:
  - `DeclarationPredicate` を `annotated(fqNames) or metaAnnotated(fqNames, includeItself = true)` で構築
  - 直接アノテーションだけでなく、メタアノテーション（アノテーションに付けたアノテーション）も対象になる
  - `FirSession.noArgPredicateMatcher` プロパティ拡張でセッションから取得できる

#### `FirNoArgConstructorGenerator` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-k2/src/FirNoArgConstructorGenerator.kt`
- **継承/実装**: `FirDeclarationGenerationExtension`
- **オーバーライドメソッド**:
  - `getCallableNamesForClass(classSymbol, context)` — `FirRegularClassSymbol` に対して `{<init>}` を返す
  - `generateConstructors(context)` — コンストラクタ生成の本体
- **動作の詳細**:
  1. `shouldGenerateNoArgConstructor()` でガード:
     - `FirRegularClassSymbol` かつ `classKind == CLASS`
     - NoArg アノテーション付きである
     - inner / local クラスでない
     - 既にゼロパラメータコンストラクタ（全パラメータにデフォルト値あり、または `@JvmOverloads` 付き）が存在しない
     - スーパークラスがゼロパラメータコンストラクタを持つか、スーパークラス自身も NoArg アノテーション付きである
  2. `createConstructor(isPrimary = false, generateDelegatedNoArgConstructorCall = false)` で生成
  3. `configureDeprecation()` でアクセシビリティを制御:
     - `@kotlin.Deprecated(level = HIDDEN)` を付与 → Kotlin コードから直接呼び出し不可
     - `@java.lang.Deprecated` を付与（利用可能な場合）→ JVM バックエンドが合成メソッドとして生成しないようにし、Java からの呼び出しを維持
     - この二重アノテーション戦略は KT-80633 / KT-80649 に対応

#### `FirNoArgCheckers` / `FirNoArgDeclarationChecker` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-k2/src/FirNoArgCheckersComponent.kt`
  / `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-k2/src/FirNoArgDeclarationChecker.kt`
- **継承/実装**:
  - `FirNoArgCheckers`: `FirAdditionalCheckersExtension`
  - `FirNoArgDeclarationChecker`: `FirRegularClassChecker(MppCheckerKind.Common)`
- **オーバーライドメソッド**: `check(declaration: FirRegularClass)`
- **動作の詳細**:
  - `classKind != CLASS` なら早期 return
  - アノテーション付きクラスに対して以下をチェック:
    - `isInner` → `NOARG_ON_INNER_CLASS_ERROR` を報告
    - `isLocal` → `NOARG_ON_LOCAL_CLASS_ERROR` を報告
    - スーパークラスがゼロパラメータコンストラクタを持たず、かつ NoArg アノテーションもない → `NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS` を報告
- **診断メッセージ** (`KtErrorsNoArg` / `DefaultErrorMessagesNoArg`):
  | 診断名 | レベル | メッセージ |
  |---|---|---|
  | `NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS` | ERROR | `Zero-argument constructor was not found in the superclass` |
  | `NOARG_ON_INNER_CLASS_ERROR` | ERROR | `Noarg constructor generation is not possible for inner classes` |
  | `NOARG_ON_LOCAL_CLASS_ERROR` | ERROR | `Noarg constructor generation is not possible for local classes` |

#### `CliNoArgDeclarationChecker` / `AbstractNoArgDeclarationChecker` (K1)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-cli/src/CliNoArgDeclarationChecker.kt`
- **継承/実装**: `DeclarationChecker`, `AnnotationBasedExtension`
- **動作の詳細**: K2 版と同等のロジックを `DeclarationDescriptor` ベースで実装。`ErrorsNoArg` (Java インターフェース) から同名の診断を報告。

#### `NoArgConstructorBodyIrGenerationExtension` (IR / K2 用)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-backend/src/NoArgConstructorBodyIrGenerationExtension.kt`
- **継承/実装**: `IrGenerationExtension`
- **オーバーライドメソッド**: `generate(moduleFragment, pluginContext)`
- **動作の詳細**:
  - `NoArgConstructorBodyIrGenerationTransformer` (IrVisitorVoid) でモジュール全体を走査
  - `origin == NO_ARG_CONSTRUCTOR_ORIGIN`（`IrDeclarationOrigin.GeneratedByPlugin(NoArgPluginKey)`）のコンストラクタのみ処理
  - スーパークラスのゼロパラメータコンストラクタを探し、`generateNoArgConstructorBody()` でボディを注入
  - `visitBody()` はオーバーライドして何もしない（ローカル宣言への適用防止）

#### `NoArgFullConstructorIrGenerationExtension` (IR / K1 用)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-backend/src/NoArgFullConstructorIrGenerationExtension.kt`
- **継承/実装**: `IrGenerationExtension`
- **動作の詳細**:
  - `visitClass()` でクラスをトラバースし `needsNoargConstructor()` を確認
  - FIR が事前にコンストラクタを生成しない K1 フローでは、IR ステージでコンストラクタ自体も `irFactory.buildConstructor {}` で生成し `declarations.add()` する
  - `noArgConstructors` キャッシュマップで再生成を防ぐ

#### `generateNoArgConstructorBody` (IR ユーティリティ)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-backend/src/noArgIrUtils.kt`
- **動作の詳細**:
  - `IrDelegatingConstructorCallImpl` でスーパーコンストラクタを呼び出す
  - `invokeInitializers = true` の場合は `IrInstanceInitializerCallImpl` も追加

#### `NoArgPluginKey`

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/noarg/noarg-backend/src/NoArgPluginKey.kt`
- **継承/実装**: `GeneratedDeclarationKey`
- **用途**: FIR で生成したコンストラクタの `origin` を IR 側で識別するためのキー (`toString() = "KotlinNoArgPlugin"`)

---

## allopen

### 登録エントリポイント

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen-cli/src/AllOpenPlugin.kt`
- クラス: `AllOpenComponentRegistrar` (`CompilerPluginRegistrar`)
- `supportsK2 = true`
- 登録内容:
  - `DeclarationAttributeAltererExtension` → `CliAllOpenDeclarationAttributeAltererExtension` (K1 用)
  - `FirExtensionRegistrar` → `FirAllOpenExtensionRegistrar` (K2 用)

### コマンドラインオプション (`AllOpenCommandLineProcessor`)

| オプション名 | 型 | 説明 |
|---|---|---|
| `annotation` | `List<String>` | 対象アノテーション FQN |
| `preset` | `List<String>` | プリセット名 |

**プリセット**:
- `spring`: `@Component`, `@Transactional`, `@Async`, `@Cacheable`, `@SpringBootTest`, `@Validated`
- `quarkus`: `@ApplicationScoped`, `@RequestScoped`
- `micronaut`: `@Around`, `@Introduction`, `@InterceptorBinding`, `@InterceptorBindingDefinitions`
- `jpa`: `@Entity`, `@Embeddable`, `@MappedSuperclass` (javax + jakarta 両対応)

### 各機能の詳細

#### `FirAllOpenExtensionRegistrar` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen-k2/src/FirAllOpenExtensionRegistrar.kt`
- **継承/実装**: `FirExtensionRegistrar`
- **オーバーライドメソッド**: `ExtensionRegistrarContext.configurePlugin()`
- **登録内容**:
  - `::FirAllOpenStatusTransformer`
  - `FirAllOpenPredicateMatcher.getFactory(...)` — アノテーション照合サービス

#### `FirAllOpenStatusTransformer` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen-k2/src/FirAllOpenStatusTransformer.kt`
- **継承/実装**: `FirStatusTransformerExtension`
- **オーバーライドメソッド**:
  - `needTransformStatus(declaration)` — 変換対象か判定
  - `transformStatus(status, declaration)` — モダリティを変更
- **動作の詳細**:
  - Java 宣言 / enhancement は変換しない (`declaration.isJavaOrEnhancement` チェック)
  - `FirRegularClass`: `classKind == CLASS` かつアノテーション付き
  - `FirCallableDeclaration`: 親クラスが上記条件を満たし、かつ `isLocal` でない
  - `transformStatus()`:
    - `status.modality == null` → `modality = OPEN, defaultModality = OPEN` に設定
    - `status.modality != null` → `defaultModality = OPEN` のみ変更（明示的な `final` を尊重）

#### `FirAllOpenPredicateMatcher` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen-k2/src/FirAllOpenStatusTransformer.kt` （同ファイル内に定義）
- **継承/実装**: `AbstractSimpleClassPredicateMatchingService`
- **動作の詳細**: noarg と同様に `annotated(fqNames) or metaAnnotated(fqNames, includeItself = true)` で述語を構築

#### `CliAllOpenDeclarationAttributeAltererExtension` / `AbstractAllOpenDeclarationAttributeAltererExtension` (K1)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/allopen/allopen-cli/src/AllOpenDeclarationAttributeAltererExtension.kt`
- **継承/実装**: `DeclarationAttributeAltererExtension`, `AnnotationBasedExtension`
- **オーバーライドメソッド**: `refineDeclarationModality(...)`
- **動作の詳細**:
  - `currentModality != FINAL` なら `null` を返し変更しない
  - 対象クラス（またはその親クラス）が特殊アノテーション付きの場合:
    - 明示的 `final` キーワードがあれば `Modality.FINAL` を維持
    - そうでなければ `Modality.OPEN` を返す
  - テスト用アノテーション定数: `ANNOTATIONS_FOR_TESTS = ["AllOpen", "AllOpen2", "test.AllOpen"]`

---

## sam-with-receiver

### 登録エントリポイント

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/sam-with-receiver/sam-with-receiver-cli/src/SamWithReceiverPlugin.kt`
- クラス: `SamWithReceiverComponentRegistrar` (`CompilerPluginRegistrar`)
- `supportsK2 = true`
- 登録内容:
  - `StorageComponentContainerContributor` → `CliSamWithReceiverComponentContributor` (JVM 限定で `SamWithReceiverResolverExtension` を登録、K1 用)
  - `FirExtensionRegistrar` → `FirSamWithReceiverExtensionRegistrar` (K2 用)

### コマンドラインオプション (`SamWithReceiverCommandLineProcessor`)

| オプション名 | 型 | 説明 |
|---|---|---|
| `annotation` | `List<String>` | 対象アノテーション FQN |
| `preset` | `List<String>` | プリセット名（現在はプリセットなし: `emptyMap()`） |

### 各機能の詳細

#### `FirSamWithReceiverExtensionRegistrar` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/sam-with-receiver/sam-with-receiver-k2/src/FirSamWithReceiverExtensionRegistrar.kt`
- **継承/実装**: `FirExtensionRegistrar`
- **オーバーライドメソッド**: `ExtensionRegistrarContext.configurePlugin()`
- **登録内容**:
  - `::FirSamWithReceiverConventionTransformer.bind(annotations)` — `bind()` でアノテーションリストを事前束縛して登録

#### `FirSamWithReceiverConventionTransformer` (FIR / K2)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/sam-with-receiver/sam-with-receiver-k2/src/FirSamWithReceiverConventionTransformer.kt`
- **継承/実装**: `FirSamConversionTransformerExtension`
- **オーバーライドメソッド**: `getCustomFunctionTypeForSamConversion(function: FirNamedFunction)`
- **動作の詳細**:
  1. 関数を含むクラスのシンボルを取得
  2. クラスが対象アノテーションを持つか確認
  3. 関数の値パラメータが0個なら `null` を返す（変換不可）
  4. パラメータリストの先頭を `receiverType` として取り出し、残りを `parameters` とした関数型を `createFunctionType()` で構築して返す
  - 例: `(A, B, C) -> R` → `A.(B, C) -> R` に変換

#### `SamWithReceiverResolverExtension` (K1)

- ファイル: `https://github.com/JetBrains/kotlin/blob/master/plugins/sam-with-receiver/sam-with-receiver-cli/src/SamWithReceiverResolverExtension.kt`
- **継承/実装**: `SamWithReceiverResolver`, `AnnotationBasedExtension`
- **オーバーライドメソッド**: `shouldConvertFirstSamParameterToReceiver(function: FunctionDescriptor)`
- **動作の詳細**:
  - 関数の含まれるクラス (`containingDeclaration as? ClassDescriptor`) が特殊アノテーションを持つなら `true` を返す
  - K1 の SAM 変換ルーティングに組み込まれ、最初のパラメータをレシーバーに変換するよう指示

---

## 共通パターン整理

| 要素 | noarg | allopen | sam-with-receiver |
|---|---|---|---|
| `CompilerPluginRegistrar` | `NoArgComponentRegistrar` | `AllOpenComponentRegistrar` | `SamWithReceiverComponentRegistrar` |
| `supportsK2` | `true` | `true` | `true` |
| K2: FIR Registrar | `FirNoArgExtensionRegistrar` | `FirAllOpenExtensionRegistrar` | `FirSamWithReceiverExtensionRegistrar` |
| K2: 主要 Extension | `FirDeclarationGenerationExtension` | `FirStatusTransformerExtension` | `FirSamConversionTransformerExtension` |
| K1: Extension | `StorageComponentContainerContributor` + `IrGenerationExtension` | `DeclarationAttributeAltererExtension` | `StorageComponentContainerContributor` |
| アノテーション照合 | `annotated or metaAnnotated` | `annotated or metaAnnotated` | クラスの `resolvedAnnotationClassIds` 直接チェック |
| 診断 | あり (3種 ERROR) | なし | なし |
| プリセット | `jpa` | `spring`, `quarkus`, `micronaut`, `jpa` | なし |

### アノテーション照合の2パターン

**FIR PredicateMatcher 方式** (noarg / allopen):

```kotlin
DeclarationPredicate.create {
    annotated(fqNames) or metaAnnotated(fqNames, includeItself = true)
}
```

- `AbstractSimpleClassPredicateMatchingService` を継承
- `FirSession.sessionComponentAccessor()` でセッションから取得可能

**直接チェック方式** (sam-with-receiver K2):

```kotlin
containingClassSymbol.resolvedAnnotationClassIds.any { it.asSingleFqName().asString() in annotations }
```

- PredicateMatcher を使わず、関数ごとに都度チェック

### K2 で FIR が生成したコンストラクタを IR で識別する仕組み (noarg 固有)

```
FIR: createConstructor(key = NoArgPluginKey, ...)
         ↓
IR: declaration.origin == IrDeclarationOrigin.GeneratedByPlugin(NoArgPluginKey)
         ↓
    visitConstructor() でボディを注入
```

K1 (`NoArgFullConstructorIrGenerationExtension`) ではこの仕組みが存在しないため、IR 段階でコンストラクタの生成とボディ注入を同時に行う。
