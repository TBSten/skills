# Source Set Separation アプローチの詳細

K1 (PSI/ComponentRegistrar) と K2 (FIR/CompilerPluginRegistrar) の大きな断絶を吸収するために使う。
Gradle が Kotlin バージョンに応じてソースディレクトリを切り替える。

## ディレクトリ構成

```
compiler-plugin/
└── src/
    ├── main/kotlin/               # 全バージョン共通 (IR 変換など)
    │   └── com/example/myplugin/
    │       ├── MyCommandLineProcessor.kt
    │       ├── MyCompilerPluginRegistrar.kt (共通エントリ)
    │       └── VersionSpecificAPI.kt        # 抽象インターフェース
    ├── v2_0_0/kotlin/             # Kotlin 2.0.0+  (K2: FIR + CompilerPluginRegistrar)
    │   └── com/example/myplugin/
    │       └── VersionSpecificAPIImpl.kt
    └── pre_2_0_0/kotlin/          # Kotlin 1.9.x-  (K1: PSI + ComponentRegistrar)
        └── com/example/myplugin/
            └── VersionSpecificAPIImpl.kt
```

さらに細かいパッチ差異がある場合は `v2_2_0/kotlin/` のように追加できる。

## VersionSpecificAPI インターフェース

`src/main/kotlin/` に定義（全バージョン共通）:

```kotlin
package com.example.myplugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration

interface VersionSpecificAPI {
    fun registerExtensions(storage: ExtensionStorage, config: CompilerConfiguration)
}
```

## バージョン別実装

### K2 向け (`src/v2_0_0/kotlin/`)

```kotlin
package com.example.myplugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.ir.backend.common.extensions.IrGenerationExtension

class VersionSpecificAPIImpl : VersionSpecificAPI {
    override fun registerExtensions(storage: ExtensionStorage, config: CompilerConfiguration) {
        with(storage) {
            FirExtensionRegistrarAdapter.registerExtension(MyFirExtensionRegistrar())
            IrGenerationExtension.registerExtension(MyIrGenerationExtension())
        }
    }
}
```

### K1 向け (`src/pre_2_0_0/kotlin/`)

```kotlin
package com.example.myplugin

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration

class VersionSpecificAPIImpl : VersionSpecificAPI {
    override fun registerExtensions(storage: ExtensionStorage, config: CompilerConfiguration) {
        // PSI ベースの拡張を登録 (K1 向け)
    }
}
```

## CompilerPluginRegistrar の共通実装

K2 向け Registrar には `supportsK2 = true` が必須:

```kotlin
// src/main/kotlin/
class MyCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(config: CompilerConfiguration) {
        // VersionSpecificAPIImpl は Gradle がソースセットを切り替えるので
        // コンパイル時には対象バージョンの実装クラスだけが存在する
        VersionSpecificAPIImpl().registerExtensions(this, config)
    }
}
```

## Gradle の動的ソースセット選択

`compiler-plugin/build.gradle.kts`:

```kotlin
val kotlinVersion = providers.environmentVariable("KOTLIN_VERSION")
    .orElse(libs.versions.kotlin)
    .get()

// バージョン文字列からソースディレクトリ名を決定
// "v2_0_0" の形式: v{major}_{minor}_{patch}
val versionDirs = file("src").listFiles { f -> f.isDirectory && f.name.matches(Regex("^(v|pre)(_\\d+)+$")) }
    ?: emptyArray()

// KOTLIN_VERSION に最も近い (かつ ≤) ディレクトリを選択
val selected = versionDirs
    .mapNotNull { dir ->
        val name = dir.name
        // "v2_0_0" → [2, 0, 0], "pre_2_0_0" → pre (K1 用)
        val isPre = name.startsWith("pre")
        val nums = name.removePrefix("v").removePrefix("pre").split("_").map { it.toIntOrNull() ?: 0 }
        Triple(dir, isPre, nums)
    }
    .let { dirs ->
        val currentNums = kotlinVersion.substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        dirs.filter { (_, isPre, nums) ->
            if (isPre) {
                // "pre_2_0_0" は current < 2.0.0 の時に使う
                compareVersions(currentNums, nums) < 0
            } else {
                // "v2_0_0" は current >= 2.0.0 の時に使う
                compareVersions(currentNums, nums) >= 0
            }
        }
        .maxByOrNull { (_, _, nums) -> nums.joinToString("") { it.toString().padStart(4, '0') } }
    }

if (selected != null) {
    kotlin.sourceSets["main"].kotlin.srcDir(selected.first)
}

fun compareVersions(a: List<Int>, b: List<Int>): Int {
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = (a.getOrElse(i) { 0 }) - (b.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}
```

## KOTLIN_VERSION 環境変数での切り替え

CI での任意バージョンテスト:

```bash
KOTLIN_VERSION=2.0.21 ./gradlew :compiler-plugin:test
KOTLIN_VERSION=2.3.20 ./gradlew :compiler-plugin:test
```

`settings.gradle.kts` に version catalog オーバーライドを追加しておくと依存バージョンも連動する:

```kotlin
// settings.gradle.kts
val kotlinOverride = providers.environmentVariable("KOTLIN_VERSION").orNull
if (kotlinOverride != null) {
    dependencyResolutionManagement {
        versionCatalogs {
            named("libs") {
                version("kotlin", kotlinOverride)
            }
        }
    }
}
```

## META-INF/services の登録

K1 と K2 の両方を登録すると、実行しているコンパイラに応じて適切な方が呼ばれる:

`src/main/resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar`:
```
com.example.myplugin.MyCompilerPluginRegistrar
```

`src/main/resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar` (K1 向け、削除予定):
```
com.example.myplugin.MyComponentRegistrar
```
