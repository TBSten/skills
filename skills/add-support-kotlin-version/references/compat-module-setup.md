# Compat Module Layer: セットアップと新モジュール追加の詳細

## アーキテクチャ概要

```
compiler-plugin/
├── compat/              # Interface + ServiceLoader Loader (全バージョン共通)
│   └── src/main/kotlin/…/compat/
│       ├── IrInjector.kt           # SPI インターフェース
│       ├── IrInjectorLoader.kt     # ServiceLoader dispatch ロジック
│       └── SimpleKotlinVersion.kt  # バージョン文字列のパース
├── compat-k2000/        # Kotlin 2.0.0–2.0.1x の実装
├── compat-k2020/        # Kotlin 2.0.20–2.1.x の実装
├── compat-k23/          # Kotlin 2.2.0+ の実装
└── build.gradle.kts     # shadow JAR で全 compat を同梱
```

## Interface モジュール (`compat/`)

### IrInjector.kt

```kotlin
package com.example.myplugin.compat

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext

// apiVersion = "2.0" でコンパイルすること (全バージョンが load 可能)
interface IrInjector {
    fun transform(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)

    interface Factory {
        /** このファクトリが対応する最低 Kotlin バージョン (e.g., "2.2.0") */
        val minVersion: String
        fun create(): IrInjector
    }
}
```

### IrInjectorLoader.kt

ServiceLoader で全 Factory を取得し、`minVersion ≤ 現在のコンパイラバージョン` の中から最大バージョンの実装を選ぶ。

```kotlin
package com.example.myplugin.compat

object IrInjectorLoader {
    fun load(): IrInjector {
        val currentVersion = SimpleKotlinVersion.current()

        val factories = buildList {
            val loader = ServiceLoader.load(
                IrInjector.Factory::class.java,
                IrInjectorLoader::class.java.classLoader,
            )
            val iter = loader.iterator()
            while (iter.hasNext()) {
                try {
                    add(iter.next())
                } catch (_: ServiceConfigurationError) {
                    // NoClassDefFoundError 等でロード不可 → このバージョンでは使えないのでスキップ
                } catch (_: NoClassDefFoundError) {
                    // 直接スロー される場合もある
                }
            }
        }

        val chosen = factories
            .map { factory -> factory to SimpleKotlinVersion.parse(factory.minVersion) }
            .filter { (_, v) -> v <= currentVersion }
            .maxByOrNull { (_, v) -> v }
            ?.first
            ?: error("No compatible IrInjector found for Kotlin $currentVersion")

        return chosen.create()
    }
}
```

### SimpleKotlinVersion.kt

```kotlin
package com.example.myplugin.compat

data class SimpleKotlinVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SimpleKotlinVersion> {
    override fun compareTo(other: SimpleKotlinVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    companion object {
        fun parse(version: String): SimpleKotlinVersion {
            // "2.1.20-RC2" のような suffix を除いて数値部分だけ取得
            val parts = version.substringBefore("-").split(".")
            return SimpleKotlinVersion(parts[0].toInt(), parts[1].toInt(), parts.getOrElse(2) { "0" }.toInt())
        }

        fun current(): SimpleKotlinVersion {
            // kotlin-compiler-embeddable の META-INF/compiler.version から読む
            val resource = SimpleKotlinVersion::class.java.classLoader
                .getResourceAsStream("META-INF/compiler.version")
            val version = resource?.bufferedReader()?.readLine()?.trim() ?: "2.0.0"
            return parse(version)
        }
    }
}
```

---

## per-version 実装モジュール (`compat-k23/` の例)

### build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        // このモジュールが担う最小バージョンの major.minor に合わせる
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    // Interface モジュールに依存 (全バージョン共通)
    compileOnly(project(":compiler-plugin:compat"))

    // このモジュールの minVersion に対応する kotlin-compiler-embeddable を pin
    compileOnly(libs.compat.embeddable.k23)  // gradle/libs.versions.toml に定義
}
```

### gradle/libs.versions.toml の追加

```toml
[versions]
compat-embeddable-k23 = "2.2.0"   # このモジュールがサポートする最小バージョン

[libraries]
compat-embeddable-k23 = { module = "org.jetbrains.kotlin:kotlin-compiler-embeddable", version.ref = "compat-embeddable-k23" }
```

### IrInjectorK23.kt

```kotlin
package com.example.myplugin.compat.k23

import com.example.myplugin.compat.IrInjector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext

internal class IrInjectorK23 : IrInjector {
    override fun transform(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // Kotlin 2.2.0+ の IR API を使った変換処理
    }

    class Factory : IrInjector.Factory {
        override val minVersion: String = "2.2.0"
        override fun create(): IrInjector = IrInjectorK23()
    }
}
```

### META-INF/services ファイル

`src/main/resources/META-INF/services/com.example.myplugin.compat.IrInjector$Factory`:
```
com.example.myplugin.compat.k23.IrInjectorK23$Factory
```

---

## Shadow JAR の設定

`compiler-plugin/build.gradle.kts` (メインモジュール) に shadow JAR 設定を追加:

```kotlin
plugins {
    id("com.gradleup.shadow") version "<latest>"
}

val bundled: Configuration by configurations.creating { isTransitive = false }

dependencies {
    compileOnly(project(":compiler-plugin:compat"))
    bundled(project(":compiler-plugin:compat-k2000"))
    bundled(project(":compiler-plugin:compat-k2020"))
    bundled(project(":compiler-plugin:compat-k23"))
    // 新しい compat-kXX を追加したらここにも追加
}

tasks.shadowJar {
    configurations = listOf(bundled)
    mergeServiceFiles()          // META-INF/services を結合 (全 Factory が ServiceLoader で発見可能に)
    archiveClassifier.set("")    // 通常の artifact に置き換え
}
```

---

## ServiceLoader dispatch の動作例

現在のコンパイラが Kotlin 2.3.20 の場合:
1. ServiceLoader が k2000 / k2020 / k23 の Factory を列挙
2. 各 Factory の minVersion をチェック: 2.0.0 ≤ 2.3.20 ✅ / 2.0.20 ≤ 2.3.20 ✅ / 2.2.0 ≤ 2.3.20 ✅
3. 最大 minVersion = "2.2.0" (k23) を選択
4. `IrInjectorK23` が実行

現在のコンパイラが Kotlin 2.0.10 の場合:
1. k23 の Factory をロードしようとすると `NoClassDefFoundError` (2.2.0 以降の API が存在しない) → スキップ
2. k2020 も同様にスキップ
3. k2000 の minVersion 2.0.0 ≤ 2.0.10 ✅ → 選択

---

## 新 compat module 追加手順のコマンド例

```bash
# 1. 最も近い既存モジュールをコピー
cp -r compiler-plugin/compat-k23 compiler-plugin/compat-k24

# 2. ソース内のパッケージ名を一括置換 (macOS の場合 -i '' が必要)
find compiler-plugin/compat-k24/src -type f -name '*.kt' -print0 | \
  xargs -0 sed -i '' 's/\.compat\.k23\./.compat.k24./g'
find compiler-plugin/compat-k24/src -type f -name '*.kt' -print0 | \
  xargs -0 sed -i '' 's/\.compat\.k23$/.compat.k24/g'

# 3. ディレクトリ名のリネーム
find compiler-plugin/compat-k24/src -type d -name 'k23' | \
  while IFS= read -r d; do mv "$d" "$(dirname "$d")/k24"; done

# 4. META-INF/services の FQN を更新
sed -i '' 's/\.k23\./\.k24\./g' \
  compiler-plugin/compat-k24/src/main/resources/META-INF/services/*
```

> **注意**: パッケージ名置換を忘れると、ServiceLoader が誤った Factory FQN を読み込む。
> 修正後、生成 JAR を `jar tf` や `unzip -p` で確認することを推奨。
