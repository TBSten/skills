# Compat Module Layer: セットアップと新モジュール追加の詳細

## アーキテクチャ概要

```
compiler-plugin/
├── compat/                       # 共有 SPI (全 compat 共通の interface)
│   └── src/main/kotlin/…/compat/
│       ├── CompatContext.kt           # SPI interface (Factory ネスト含む)
│       ├── CompatContextLoader.kt     # ServiceLoader dispatch
│       ├── KotlinToolingVersion.kt    # Maturity 付き version 比較 (references/kotlin-tooling-version.md)
│       ├── IrDeclarationOriginCompat.kt # reflection shim 例 (references/reflection-shim.md)
│       └── IrAnnotationCompat.kt      # reflection shim 例
├── compat-k210/                  # Kotlin 2.1.20+
├── compat-k222/                  # Kotlin 2.2.x      (CompatContext by k210.CompatContextImpl())
├── compat-k2220/                 # Kotlin 2.2.20+    (CompatContext by k222.CompatContextImpl())
├── compat-k230/                  # Kotlin 2.3.0+     (CompatContext by k2220.CompatContextImpl())
├── compat-k2320/                 # Kotlin 2.3.20+    (capability flag を on にする)
├── compat-k2321/                 # Kotlin 2.3.21+    (KLIB cross-module hint を on にする)
├── compat-k240_beta2/            # Kotlin 2.4.0-Beta2+ (IrAnnotationImpl 差分吸収)
└── build.gradle.kts              # shadow JAR で全 compat module を同梱、 ServiceLoader でメタデータ merge
```

各 compat module は `compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:X.Y.Z")` を独自に pin する。 ShadowJar が main 実装 + 全 compat module を 1 jar にバンドルし、 ServiceLoader が runtime version に合致した Factory を選択する。

## Interface モジュール (`compat/`)

### CompatContext.kt

```kotlin
package me.tbsten.compose.preview.lab.compiler.compat

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

/**
 * SPI that absorbs version-specific differences in the Kotlin compiler API.
 *
 * Implementations are registered as a [Factory] in
 * `META-INF/services/.../CompatContext$Factory` and [CompatContextLoader] picks
 * the best one at runtime based on the current Kotlin compiler version.
 *
 * When a new Kotlin version introduces API drift, add an abstract method here
 * and implement it in each compat module ([Factory.minVersion]).
 */
public interface CompatContext {
    // ----------------------------- API shim methods -----------------------------
    // 「呼び出し signature だけがバージョン間で違う」 ものをここで absorb する。
    // 各 compat module は自分の baseline Kotlin で compile されるので、
    // 生成 bytecode は対応する JVM signature を参照する。

    public fun irCall(builder: IrBuilderWithScope, callee: IrSimpleFunctionSymbol): IrCall

    public fun addConstructorCallAnnotation(
        function: IrSimpleFunction,
        type: IrType,
        constructorSymbol: IrConstructorSymbol,
    )

    public fun transformModuleFragment(moduleFragment: IrModuleFragment, transformer: Any)

    // ----------------------------- Capability flags -----------------------------
    // 「特定 API クラスが存在するか / 安定しているか」 を真偽値で公開する。
    // main 実装は flag を見て extension の登録自体をスキップできるので、
    // 古い Kotlin でも plugin が startup する。
    // 詳細は references/version-gating.md。

    /**
     * Returns whether `FirDeclarationGenerationExtension.getTopLevelClassIds` API
     * is stable (Kotlin 2.3.20+).
     *
     * Implementations:
     * - `compat-k210` / `compat-k222` / `compat-k2220` / `compat-k230` → `false`
     * - `compat-k2320` / `compat-k2321` / `compat-k240_beta2` → `true`
     */
    public fun supportsFirHintGeneration(): Boolean

    /**
     * Returns whether `FirNamedFunction` exists (Kotlin 2.3.20+) so the FIR
     * Checker extension can be loaded without `NoClassDefFoundError`.
     *
     * Implementations: same threshold as [supportsFirHintGeneration].
     */
    public fun supportsFirCheckers(): Boolean

    /**
     * Returns whether `referenceFunctions`-based cross-module hint discovery is
     * safe on KLIB targets (JS/Wasm/iOS/native). Requires Kotlin 2.3.21+ (KT-82395 fix).
     *
     * Implementations:
     * - pre-2.3.21 → `false`
     * - `compat-k2321` / `compat-k240_beta2` → `true`
     */
    public fun supportsKlibCrossModuleHint(): Boolean

    // ----------------------------- Factory + load -------------------------------

    public interface Factory {
        /** Minimum Kotlin version this implementation supports (e.g. "2.3.0", "2.4.0-Beta2"). */
        public val minVersion: String
        public fun create(): CompatContext
    }

    public companion object {
        public fun load(knownVersion: KotlinToolingVersion? = null): CompatContext =
            CompatContextLoader.load(knownVersion)
    }
}
```

### CompatContextLoader.kt

ServiceLoader で全 Factory を取得し、`minVersion ≤ 現在のコンパイラバージョン` の中で最大 minVersion の Factory を選ぶ。 `META-INF/compiler.version` (= `kotlin-compiler-embeddable` jar が同梱) から現在の Kotlin 版を読む。

```kotlin
package me.tbsten.compose.preview.lab.compiler.compat

import java.util.ServiceLoader

internal object CompatContextLoader {
    fun load(knownVersion: KotlinToolingVersion? = null): CompatContext {
        val factories = ServiceLoader
            .load(CompatContext.Factory::class.java, CompatContext.Factory::class.java.classLoader)
            .toList()

        if (factories.isEmpty()) {
            error(
                "No CompatContext.Factory implementations found. " +
                    "The :compiler-plugin:compat-k* modules may not be bundled into the published plugin jar.",
            )
        }

        val currentVersion = knownVersion
            ?: detectCurrentKotlinVersion()
            ?: error("Could not detect Kotlin compiler version (META-INF/compiler.version missing). " +
                "Available: ${factories.map { it.minVersion }}")

        val compatible = factories.filter { factory ->
            runCatching { currentVersion >= KotlinToolingVersion(factory.minVersion) }
                .getOrDefault(false)
        }

        val best = compatible.maxByOrNull { KotlinToolingVersion(it.minVersion) }
            ?: error("No compatible CompatContext.Factory for Kotlin $currentVersion. " +
                "Available: ${factories.map { it.minVersion }}")

        return best.create()
    }

    private fun detectCurrentKotlinVersion(): KotlinToolingVersion? {
        val loader = CompatContextLoader::class.java.classLoader ?: return null
        val text = loader.getResourceAsStream("META-INF/compiler.version")
            ?.use { it.bufferedReader().readText().trim() }
            ?: return null
        if (text.isEmpty()) return null
        return runCatching { KotlinToolingVersion(text) }.getOrNull()
    }
}
```

`KotlinToolingVersion` の Maturity 付き比較 (STABLE > RC > BETA > ALPHA > MILESTONE > DEV > SNAPSHOT) は `references/kotlin-tooling-version.md` を参照。

### compat/build.gradle.kts

```kotlin
plugins {
    id("convention-jvm")
    id("convention-format")
}

dependencies {
    compileOnly(libs.kotlinCompilerEmbeddable)

    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotlinCompilerEmbeddable)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // kctfork bundles a kotlin-compiler-embeddable that cannot parse Java 26 version
    // strings, so the test JVM has to run on Java 21.
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
```

---

## per-version 実装モジュール (delegation pattern)

### 推奨パターン: `by` で委譲し差分だけ override

```kotlin
// compiler-plugin/compat-k2320/src/main/kotlin/.../compat/k2320/CompatContextImpl.kt
package me.tbsten.compose.preview.lab.compiler.compat.k2320

import me.tbsten.compose.preview.lab.compiler.compat.CompatContext
import me.tbsten.compose.preview.lab.compiler.compat.k230.CompatContextImpl as K230Impl

/**
 * Kotlin 2.3.20+ implementation.
 *
 * Delegates everything to the 2.3.x impl ([K230Impl]) and only overrides the
 * capability flags that turn on with 2.3.20 (FirNamedFunction class introduced,
 * FirDeclarationGenerationExtension top-level API stable).
 */
class CompatContextImpl : CompatContext by K230Impl() {
    override fun supportsFirHintGeneration(): Boolean = true
    override fun supportsFirCheckers(): Boolean = true

    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.3.20"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
```

これにより `compat-k2320` の本体は **2 メソッド + Factory** だけで済む。 巨大な copy-paste を避けられる。

### build.gradle.kts

```kotlin
plugins {
    id("convention-jvm")
    id("convention-format")
}

dependencies {
    compileOnly(project(":compiler-plugin:compat"))
    compileOnly(project(":compiler-plugin:compat-k230"))   // ← delegation target

    // この compat module の baseline Kotlin を pin
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.20")
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        // この module の baseline minor に合わせる
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
    }
}
```

> **注意**: delegation の `K230Impl()` をコンストラクト時に embeddable jar が classpath に居る必要がある。 `compileOnly(project(":compiler-plugin:compat-k230"))` で十分 (実行時は ShadowJar に bundle される)。

### META-INF/services

`compat-k2320/src/main/resources/META-INF/services/me.tbsten.compose.preview.lab.compiler.compat.CompatContext$Factory`:

```
me.tbsten.compose.preview.lab.compiler.compat.k2320.CompatContextImpl$Factory
```

> ファイル名の `$` はディレクトリ上では literal `$` (escape 不要)。 commit 時に `git` が怒ったら quoting を確認。

### settings.gradle.kts に追加

```kotlin
include(":compiler-plugin:compat-k2320")
```

### main `compiler-plugin/build.gradle.kts` に追加

```kotlin
val embedded by configurations.dependencyScope("embedded")
val embeddedClasspath by configurations.resolvable("embeddedClasspath") { extendsFrom(embedded) }

configurations.named("compileOnly").configure { extendsFrom(embedded) }
configurations.named("testImplementation").configure { extendsFrom(embedded) }

dependencies {
    add(embedded.name, projects.compilerPlugin.compat)
    add(embedded.name, projects.compilerPlugin.compatK210)
    add(embedded.name, projects.compilerPlugin.compatK222)
    add(embedded.name, projects.compilerPlugin.compatK2220)
    add(embedded.name, projects.compilerPlugin.compatK230)
    add(embedded.name, projects.compilerPlugin.compatK2320)   // ← 新規行
    add(embedded.name, projects.compilerPlugin.compatK2321)
    add(embedded.name, projects.compilerPlugin.compatK240Beta2)
}
```

typesafe accessor は kebab → camel に変換される: `compat-k2320` → `projects.compilerPlugin.compatK2320`、 `compat-k240_beta2` → `projects.compilerPlugin.compatK240Beta2`。

---

## Shadow JAR の設定

`compiler-plugin/build.gradle.kts` の核心部:

```kotlin
plugins {
    alias(libs.plugins.conventionJvm)
    alias(libs.plugins.conventionPublish)
    alias(libs.plugins.shadow)
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = ""
    from(java.sourceSets["main"].output)
    configurations = listOf(embeddedClasspath)

    // ★ 全 compat module の META-INF/services/.../CompatContext$Factory を merge する
    mergeServiceFiles()

    // Kotlin 本体は IDE/Gradle 側が提供する。bundle すると衝突するので除外
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains:annotations"))
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// 既定の jar task は disable (shadowJar が公開 artifact になる)
tasks.named<Jar>("jar") { enabled = false }

// apiElements / runtimeElements を shadowJar に置き換える
arrayOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name).configure { artifacts.removeIf { true } }
    artifacts.add(name, shadowJar)
}

tasks.named("assemble") { dependsOn(shadowJar) }
```

`mergeServiceFiles()` が必須。 各 compat module の `META-INF/services/.../CompatContext$Factory` ファイルを 1 つに連結することで、 ServiceLoader が全 Factory を発見できる。

---

## ServiceLoader dispatch の動作例

現在のコンパイラが Kotlin 2.3.20 の場合:

1. ServiceLoader が k210 / k222 / k2220 / k230 / k2320 / k2321 / k240_beta2 の Factory を列挙
2. minVersion フィルタ: `2.1.20 ≤ 2.3.20 ✅` `2.2.0 ≤ 2.3.20 ✅` `2.2.20 ≤ 2.3.20 ✅` `2.3.0 ≤ 2.3.20 ✅` `2.3.20 ≤ 2.3.20 ✅` `2.3.21 ≤ 2.3.20 ✗` `2.4.0-Beta2 ≤ 2.3.20 ✗`
3. 最大 minVersion = "2.3.20" → `compat-k2320` の `CompatContextImpl` が選択
4. `supportsFirHintGeneration()` = `true` で FIR generator が registerExtensions される

現在のコンパイラが Kotlin 2.1.21 の場合:

1. 同じ列挙 → minVersion `≤ 2.1.21` を満たす Factory は `compat-k210` のみ
2. → `compat-k210` の impl が選択
3. `supportsFirHintGeneration()` = `false` で FIR generator は register されない (= startup 失敗を回避)

---

## 新 compat module 追加手順のコマンド例

```bash
# 1. 最も近い既存モジュールをコピー
cp -r compiler-plugin/compat-k230 compiler-plugin/compat-k2320

# 2. ソース内のパッケージ名を一括置換 (macOS の場合 -i '' が必要、 Linux は -i)
find compiler-plugin/compat-k2320/src -type f \( -name '*.kt' -o -name '*' \) -print0 | \
  xargs -0 sed -i '' 's/\.compat\.k230\b/.compat.k2320/g'

# 3. ディレクトリ名のリネーム
find compiler-plugin/compat-k2320/src -type d -name 'k230' | \
  while IFS= read -r d; do mv "$d" "$(dirname "$d")/k2320"; done

# 4. META-INF/services の FQN を更新
sed -i '' 's/\.k230\./\.k2320\./g' \
  compiler-plugin/compat-k2320/src/main/resources/META-INF/services/*

# 5. build.gradle.kts の compileOnly kotlin-compiler-embeddable を新 baseline に
sed -i '' 's|kotlin-compiler-embeddable:2\.3\.0|kotlin-compiler-embeddable:2.3.20|' \
  compiler-plugin/compat-k2320/build.gradle.kts

# 6. Factory.minVersion を更新
sed -i '' 's|"2\.3\.0"|"2.3.20"|' \
  compiler-plugin/compat-k2320/src/main/kotlin/**/CompatContextImpl.kt

# 7. delegation target を新しい既存 module に向ける
# (手動編集が必要: `CompatContext by k230.CompatContextImpl()` → `by k2320.CompatContextImpl()` 等)
```

> **注意**: パッケージ名置換を忘れると、 ServiceLoader が誤った Factory FQN を読み込む。 修正後、 生成 JAR を `jar tf compiler-plugin/build/libs/*.jar | grep CompatContext` で確認することを推奨。

---

## 小さな accessor 差分は reflection で吸収する

「enum companion vs static field GET」 のように同名 method の **dispatch shape** が patch で変わるだけの場合、 新 compat module を作らずに `compat/` の reflection-based shim で吸収できる。

例 (`IrDeclarationOriginCompat.kt`):

```kotlin
object IrDeclarationOriginCompat {
    /** `IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA` を runtime に解決 */
    val LOCAL_FUNCTION_FOR_LAMBDA: IrDeclarationOrigin by lazy {
        resolveOrigin("LOCAL_FUNCTION_FOR_LAMBDA")
    }

    private fun resolveOrigin(name: String): IrDeclarationOrigin {
        val clazz = Class.forName("org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin")
        // 2.3.0/10: Companion.getX() / 2.3.20+: static field GET — どちらでも引ける順序で試す
        runCatching {
            val companionField = clazz.getDeclaredField("Companion")
            val companion = companionField.get(null)
            val getter = companion.javaClass.getMethod("get${name.toCamel()}")
            return getter.invoke(companion) as IrDeclarationOrigin
        }
        runCatching {
            val field = clazz.getDeclaredField(name)
            return field.get(null) as IrDeclarationOrigin
        }
        error("Cannot resolve IrDeclarationOrigin.$name on this Kotlin version")
    }
}
```

詳細は `references/reflection-shim.md`。
