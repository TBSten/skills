# Collector Compiler Plugin アーキテクチャ

## 全体像

```
[Module A のコンパイル]
  FIR: @Collectable 検出 → hint 関数スタブ生成 (FirDeclarationGenerationExtension)
  IR:  hint 関数 body 生成 → registerFunctionAsMetadataVisible()
  → JAR メタデータに hint 関数が含まれる

[Module B のコンパイル (Module A に依存)]
  IR:  referenceFunctions(CallableId) で hint パッケージを検索
       → パラメータ型から収集対象クラスを復元
       → @CollectAll 関数/プロパティの body を生成
         listOf(ClassA::class, ClassB::class, ...)
```

## モジュール構成

```
project/
├── annotations/          # ユーザー向け API (runtime dependency)
│   └── src/commonMain/kotlin/<package>/
│       ├── Collectable.kt    # 収集対象マーカー
│       └── CollectAll.kt     # 収集結果注入
├── compiler-plugin/      # Compiler Plugin 本体 (compile-time only)
│   ├── src/main/kotlin/<package>/compiler/
│   │   ├── PluginRegistrar.kt
│   │   ├── fir/
│   │   │   ├── FirExtensionRegistrar.kt
│   │   │   └── HintFirGenerator.kt
│   │   └── ir/
│   │       ├── IrExtension.kt
│   │       ├── HintBodyGenerator.kt
│   │       ├── Discovery.kt
│   │       └── CollectAllTransformer.kt
│   ├── src/main/resources/META-INF/services/
│   │   └── org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
│   └── src/test/kotlin/<package>/compiler/
│       └── CollectorTest.kt
└── gradle-plugin/        # Gradle Plugin
    ├── src/main/kotlin/<package>/gradle/
    │   └── CollectorGradlePlugin.kt
    └── src/main/resources/META-INF/gradle-plugins/
        └── <plugin-id>.properties
```

## hint 関数フォーマット

| 項目 | 値 |
|------|-----|
| パッケージ | `<base-package>.hints` |
| 関数名 | `collect_<tag>` (tag 未指定時は `collect_default`) |
| シグネチャ | `fun collect_<tag>(collected: <TargetClass>): Unit` |
| パラメータ | 1つ: `collected` — 型が収集対象クラス |
| 戻り値 | `Unit` |
| Visibility | ソースクラスの visibility を継承 |

### 例

Module A に以下がある場合:
```kotlin
@Collectable(tag = "handler")
class UserHandler { ... }

@Collectable(tag = "handler")
class OrderHandler { ... }
```

以下の hint 関数が生成される:
```kotlin
// package: com.example.collector.hints
fun collect_handler(collected: UserHandler): Unit = error("stub")
fun collect_handler(collected: OrderHandler): Unit = error("stub")
```

Module B で以下を書くと:
```kotlin
@CollectAll(tag = "handler")
fun allHandlers(): List<KClass<*>>
```

コンパイラが body を生成:
```kotlin
fun allHandlers(): List<KClass<*>> = listOf(UserHandler::class, OrderHandler::class)
```

## 各コンポーネントの責務

### CompilerPluginRegistrar

- `supportsK2 = true` を宣言
- `FirExtensionRegistrarAdapter` で FIR Extension を登録
- `IrGenerationExtension` で IR Extension を登録

### FIR Extension: HintFirGenerator

`FirDeclarationGenerationExtension` を実装。

1. `getTopLevelCallableIds()` — hint パッケージ内の関数 ID を返す
2. `generateTopLevelFunctions()` — hint 関数のスタブ (`FirSimpleFunction`) を生成

**predicate**: `@Collectable` アノテーション付きクラスを検出

### IR Extension: IrExtension

`IrGenerationExtension` を実装。3 フェーズで動作:

**Phase 1: Hint Body 生成**
- FIR で生成されたスタブに空 body を追加
- `registerFunctionAsMetadataVisible()` で downstream に公開

**Phase 2: Discovery**
- `referenceFunctions(CallableId("hints-package", "collect_<tag>"))` で hint 関数を検索
- 各 hint 関数の `valueParameters[0].type` から収集対象クラスを抽出
- visibility フィルタ (public は常に、internal は friend module のみ)

**Phase 3: @CollectAll 変換**
- `@CollectAll` 付き関数/プロパティを検出
- Discovery 結果を元に `listOf(A::class, B::class, ...)` を body として生成

## 必須 Kotlin Compiler API

```kotlin
// FIR
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate

// IR
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

// Cross-module visibility
import org.jetbrains.kotlin.backend.common.extensions.IrGeneratedDeclarationsRegistrar

// Function lookup
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
```

## Incremental Compilation 対応

- hint 関数とソースクラスの依存関係を `LookupTracker` で記録
- ソースクラスの追加/削除時に hint 関数が再生成される
- downstream の `@CollectAll` も再コンパイルされる
