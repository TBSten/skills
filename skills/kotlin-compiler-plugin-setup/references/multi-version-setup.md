# Multi-Kotlin Version Support: 初期セットアップ

Kotlin Compiler Plugin で複数 Kotlin バージョンを 1 つの JAR でサポートする。
バージョン追加・削除の継続的な作業は `add-support-kotlin-version` スキルを使う。

---

## 戦略の選択

### タンデムリリース vs 独立リリース

| 戦略 | 実装コスト | 利用者の手間 | 採用例 |
|------|-----------|------------|--------|
| **タンデム** (プラグイン = Kotlin バージョン) | 低 | Kotlin 更新のたびにプラグインも更新 | kotlinx.serialization, Jetpack Compose |
| **独立** (1 JAR で複数バージョン対応) | 高 | プラグインバージョンを気にしなくてよい | Metro, kitakkun/Kondition |

独立リリースを選んだ場合、以下のアーキテクチャを選択する。

---

## アーキテクチャ A: Source Set Separation

**向き**: K1 (PSI/ComponentRegistrar) と K2 (FIR/CompilerPluginRegistrar) の両対応が必要な場合

```
compiler-plugin/
└── src/
    ├── main/kotlin/          # 共通 (IR 変換など)
    ├── v2_0_0/kotlin/        # K2 向け (FIR + CompilerPluginRegistrar)
    └── pre_2_0_0/kotlin/     # K1 向け (PSI + ComponentRegistrar)
```

1. `VersionSpecificAPI` インターフェースを `src/main/kotlin/` に定義
2. K2 向け実装を `src/v2_0_0/kotlin/` に、K1 向けを `src/pre_2_0_0/kotlin/` に配置
3. `build.gradle.kts` で `KOTLIN_VERSION` 環境変数またはバージョンカタログ値を読み、適合するソースディレクトリを `sourceSets["main"].kotlin.srcDir(...)` で追加
4. K2 向け `CompilerPluginRegistrar` に `supportsK2 = true` を設定

詳細コード: `add-support-kotlin-version` スキルの `references/source-set-separation.md`

---

## アーキテクチャ B: Compat Module Layer (metro スタイル)

**向き**: K2+ を対象に、パッチバージョン間の IR API 差異を吸収したい場合

```
compiler-plugin/
├── compat/             # Interface + ServiceLoader Loader (apiVersion=2.0)
├── compat-k2000/       # Kotlin 2.0.0–2.0.1x の IR 実装
├── compat-k2020/       # Kotlin 2.0.20–2.1.x の IR 実装
├── compat-k23/         # Kotlin 2.2.0+ の IR 実装
└── build.gradle.kts    # shadow JAR で全 compat を同梱 (mergeServiceFiles)
```

### compat/ Interface モジュールの作成

```kotlin
// apiVersion = "2.0" でコンパイル
interface IrInjector {
    fun transform(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)

    interface Factory {
        val minVersion: String
        fun create(): IrInjector
    }
}
```

ServiceLoader dispatch ロジック: `minVersion ≤ 現在の Kotlin バージョン` の中から最大のものを選択。ロード時に `NoClassDefFoundError` が発生した Factory は自動でスキップ（= バージョン非対応の実装が安全に無視される）。

### Shadow JAR 設定

```kotlin
val bundled: Configuration by configurations.creating { isTransitive = false }
dependencies {
    bundled(project(":compiler-plugin:compat-k2000"))
    bundled(project(":compiler-plugin:compat-k23"))
}
tasks.shadowJar {
    configurations = listOf(bundled)
    mergeServiceFiles()   // META-INF/services を結合
    archiveClassifier.set("")
}
```

詳細コード: `add-support-kotlin-version` スキルの `references/compat-module-setup.md`

---

## CI マトリクスの初期設定

```yaml
jobs:
  test:
    strategy:
      fail-fast: false
      matrix:
        kotlin: ["2.0.0", "2.1.21", "2.2.20", "2.3.20"]
    steps:
      - run: ./gradlew :compiler-plugin:test -Ptest.kotlin=${{ matrix.kotlin }}
```

kctfork バージョンマップと CI YAML の詳細: `add-support-kotlin-version` スキルの `references/ci-matrix.md`

---

## バイナリ互換性 (BCV)

ランタイムライブラリの公開 API を Binary Compatibility Validator (BCV) で保護する:

```kotlin
// runtime/build.gradle.kts
plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}
```

`./gradlew apiDump` で API snapshot を生成し、PR で意図しない破壊的変更を検知する。

---

## 実装チェックリスト（初期セットアップ）

- [ ] タンデム / 独立 どちらか決定
- [ ] アーキテクチャ A / B どちらか決定
- [ ] Interface モジュール作成 (B の場合)
- [ ] per-version 実装モジュール作成 (B) またはソースセット設定 (A)
- [ ] Shadow JAR 設定 (B の場合)
- [ ] CI マトリクス設定
- [ ] kctfork バージョンマップ設定
- [ ] BCV 設定 (runtime がある場合)
- [ ] 全対象バージョンでテスト GREEN 確認

バージョン追加・削除の継続作業 → `add-support-kotlin-version` スキルへ
