# Capability Flags & Test Self-Skip

## なぜ「capability flag」 が要るか

API shim だけで全 Kotlin バージョンに対応できないケースがある:

1. **新クラスが導入された場合** — 例えば `FirNamedFunction` は Kotlin 2.3.20 で新規 import 可能になった。 plugin の checker class が `Set<FirDeclarationChecker<FirNamedFunction>>` を field 持つだけで、 JVM classloader が 2.3.10 以下で plugin を load した瞬間 `NoClassDefFoundError` で **plugin startup 自体が失敗** する。
2. **不安定な extension point** — `FirDeclarationGenerationExtension.getTopLevelClassIds` は 2.3.20 で stable 化。 それ以前は signature が変動するため拡張を register しても crash の可能性。
3. **incremental compile の bug** — KT-82395 (KLIB IC で top-level declaration が stale cache に残る) は 2.3.21+ で fix。 2.3.20 では FIR generator は動くが KLIB の cross-module reference が壊れる。

これらは「method signature が違う」 ではなく 「**この機能が安全に使えるかどうか**」 の判定なので、 method shim ではなく **真偽値の capability flag** として `CompatContext` に乗せる。

## 設計原則

1. **flag 名は `supportsXxx()`** で動詞句 — 機能を主語にする
2. **KDoc に implementations 真偽表を必ず書く** — 何が `true` で何が `false` か一目で分かる
3. **default 実装を持たせない** — 各 compat module で必ず明示。 delegation chain が長くなったとき override 漏れを避ける (interface に default false を入れたい衝動を抑える)
4. **main 実装側は flag を見て「extension を register しない」 を選ぶ** — register してから try-catch するのは NG (JVM verifier が class load で死ぬ)
5. **platform 軸が絡む場合は flag を 2 つに割る** — 例えば JVM では使えるが KLIB では使えない場合、 `supportsFirHintGeneration()` (JVM/KLIB 共通) と `supportsKlibCrossModuleHint()` (KLIB のみ追加制約) を分ける

## 例: `supportsFirCheckers()`

```kotlin
public interface CompatContext {
    /**
     * Returns whether the runtime Kotlin compiler exposes the
     * `org.jetbrains.kotlin.fir.declarations.FirNamedFunction` declaration class — i.e.
     * whether `PreviewLabFirCheckersExtension` (`FirAdditionalCheckersExtension` whose
     * `simpleFunctionCheckers` field is typed `Set<FirDeclarationChecker<FirNamedFunction>>`)
     * can be loaded without `NoClassDefFoundError` at plugin startup.
     *
     * `FirNamedFunction` was introduced in Kotlin 2.3.20 (it superseded the `FirSimpleFunction`
     * type as the parameterization for `simpleFunctionCheckers`). Earlier Kotlin versions
     * (2.1.x / 2.2.x / 2.3.0–2.3.10) ship a different class hierarchy and the JVM classloader
     * fails fast when it sees the `FirNamedFunction` type reference inside our checker class.
     *
     * Without this gate, the entire compiler plugin fails to start on those Kotlin versions —
     * even for tests / production code that have no need for the checkers.
     *
     * The checker logic itself is functionally optional: validation also happens at IR-pass
     * time, which catches the same constraint violations on Kotlin versions where the FIR
     * checker is skipped. Skipping the checker registration on pre-2.3.20 therefore degrades
     * only the IDE-side red-squiggly experience (errors still fire at compile time).
     *
     * Implementations:
     * - `compat-k210` / `compat-k222` / `compat-k2220` / `compat-k230` → `false`
     * - `compat-k2320` (Kotlin 2.3.20+) / `compat-k2321` / `compat-k240_beta2` → `true`
     */
    public fun supportsFirCheckers(): Boolean
}
```

main 実装側:

```kotlin
class PreviewLabFirExtensionRegistrar(private val config: PluginConfig) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        val compat = CompatContext.load()
        +::PreviewLabFirBuiltIns
        +::HintEntriesProvider
        +::PreviewLabFirStatusTransformerExtension

        if (compat.supportsFirCheckers()) {
            +::PreviewLabFirCheckersExtension       // ← gating ★
        }
        if (compat.supportsFirHintGeneration() && config.collectPreviewsEnabled) {
            +::PreviewHintFirGenerator               // ← gating ★
        }
    }
}
```

## 例: platform 軸付き flag (`supportsKlibCrossModuleHint`)

JVM/Android では 2.3.20 以降全部使える機能が、 JS/Wasm/iOS では 2.3.21 まで待つ必要があるとき:

```kotlin
public interface CompatContext {
    /**
     * Returns whether `IrPluginContext.referenceFunctions(...)`-based cross-module hint
     * discovery is **safe to use on KLIB targets** (JS / Wasm / iOS / native).
     *
     * On JVM / Android this works on every Kotlin version that supports
     * [supportsFirHintGeneration] (= 2.3.20+). On KLIB-based targets, the
     * `referenceFunctions` walk only became safe after KT-82395 was fixed in
     * Kotlin 2.3.21 / 2.4.0-Beta2+.
     *
     * Callers should be platform-aware:
     * - JVM / Android → `supportsFirHintGeneration()` で十分
     * - KLIB target → 加えてこの flag も `true` が必要
     *
     * Implementations:
     * - `compat-k210` / `compat-k222` / `compat-k2220` / `compat-k230` / `compat-k2320` → `false`
     * - `compat-k2321` / `compat-k240_beta2` → `true`
     */
    public fun supportsKlibCrossModuleHint(): Boolean
}
```

call site (IR pass):

```kotlin
class PreviewLabIrGenerationExtension(private val config: PluginConfig) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val compat = CompatContext.load()
        val isKlibTarget = pluginContext.platform.requiresKlibIcSafetyForCrossModuleHint()

        val crossModuleSafe = compat.supportsFirHintGeneration() &&
            (if (isKlibTarget) compat.supportsKlibCrossModuleHint() else true)

        if (crossModuleSafe) {
            // collectAllModulePreviews() を有効化
        } else {
            // collectModulePreviews() (自モジュール限定) のみ
            // 必要なら user-facing warning も出す
        }
    }
}
```

## テストの self-skip (capability flag と対応する)

CI matrix で `fail-fast: false` × 全 Kotlin 版を回すとき、 各テストは自分が走れる版で **self-skip** する。 「matrix 側でテストを除外する」 (例: `if: matrix.kotlin >= '2.3.20'`) よりも **テスト側で skip** したほうが local 実行とも整合する。

`KotlinToolingVersion` に `isAtLeast(...)` を生やしておく:

```kotlin
fun KotlinToolingVersion.isAtLeast(major: Int, minor: Int, patch: Int): Boolean =
    this >= KotlinToolingVersion(major = major, minor = minor, patch = patch, classifier = null)
```

テスト基底クラスで:

```kotlin
abstract class CompilerPluginTestBase : FunSpec() {
    protected val currentKotlin: KotlinToolingVersion = run {
        val v = System.getProperty("test.kotlin.version") ?: "0.0.0"
        KotlinToolingVersion(v)
    }

    protected fun TestScope.skipIfBefore(major: Int, minor: Int, patch: Int) {
        if (!currentKotlin.isAtLeast(major, minor, patch)) {
            // Kotest 経由で test を skip
            this@CompilerPluginTestBase.config(enabled = false)
        }
    }
}

class CrossModuleAggregationKlibTest : CompilerPluginTestBase({
    test("collectAllModulePreviews works across KLIB modules") {
        skipIfBefore(2, 3, 21)
        // ... actual assertion
    }
})
```

`./scripts/compiler-plugin-test.sh 2.3.20` で走らせた場合、 上記テストは self-skip され Kotest report に「skipped」 として出る。

## チェックリスト (新 flag 追加時)

- [ ] flag 名は `supportsXxx()` で動詞句
- [ ] KDoc に **どの compat module で true/false** か明示 (= `Implementations:` セクション)
- [ ] **なぜ flag が必要か** (= JVM classloader が NCDFE で死ぬ / KT-xxxxx の bug が fix されたバージョン以降のみ安全、 etc) を KDoc に書く
- [ ] **flag false 時の degrade 内容** を KDoc に書く (= 「FIR checker は skip されるが IR pass で同等 validation が走る」 等)
- [ ] 各 compat module で **default に頼らず明示的に override** (delegation chain で透過されることを明示するために、 false 側でも一度 override しておく方が安全)
- [ ] main 実装側で flag を **register より前** に評価する (register-then-catch は JVM verifier が許さない)
- [ ] 対応する self-skip ヘルパ (`skipIfBefore(...)`) をテスト基底に追加
