---
name: kotlin-compiler-plugin-setup
description: >
  Sets up a Kotlin Compiler Plugin project with multi-module Gradle structure.
  Creates buildSrc convention plugins (kotlin-jvm), compiler-plugin module with
  AutoService + KSP for plugin registration, Gradle plugin wrapper
  (KotlinCompilerPluginSupportPlugin), runtime API module (Kotlin Multiplatform),
  unit tests with kctfork (KotlinCompilation testing) + Kotest, and integration
  test module using kotlinCompilerPluginClasspath.
  Targets Kotlin 2.x K2 compiler with FIR + IR extension architecture.
  Use when requested: "Kotlin compiler plugin を作りたい", "compiler plugin のプロジェクトをセットアップ",
  "setup kotlin compiler plugin", "KotlinCompilation でテストしたい",
  "compiler plugin の unit test を書きたい", "FIR/IR extension のプロジェクト構成",
  "Gradle plugin で compiler plugin をラップ", "compiler plugin の integration test をセットアップ".
---

# Kotlin Compiler Plugin Setup

Kotlin Compiler Plugin のマルチモジュールプロジェクトを一式セットアップする。

## Usage

### 確認事項

セットアップ前に以下を確認する。ユーザーの指示から明確に読み取れる項目は確認を省略してよい。

1. **プロジェクト名** — ルートプロジェクト名 (kebab-case)
2. **Group ID** — Maven artifact の groupId (例: `com.example.myplugin`)
3. **Plugin ID** — Kotlin compiler plugin の ID (例: `com.example.myplugin`)。通常 groupId と同じ
4. **セットアップ範囲** — 以下から選択 (デフォルト: 全て)
   - [x] buildSrc convention plugins
   - [x] compiler-plugin module (FIR + IR)
   - [x] gradle-plugin module (KotlinCompilerPluginSupportPlugin)
   - [x] runtime module (Kotlin Multiplatform)
   - [x] Unit tests (kctfork + Kotest)
   - [x] Integration test module
5. **Kotlin バージョン** — デフォルト: 最新安定版
6. **Java toolchain バージョン** — デフォルト: 21

## セットアップ手順

### Step 1: プロジェクトルートの作成

`settings.gradle.kts` を作成する。`example/settings.gradle.kts` を参考にプロジェクト名とモジュール構成を設定。

```
<project-root>/
├── buildSrc/
├── compiler-plugin/
├── gradle-plugin/
├── runtime/
├── integration-test/
│   ├── test-jvm/          # JVM 単体の E2E テスト
│   └── test-kmp/          # KMP (JVM + JS) の E2E テスト
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

### Step 2: Version Catalog の作成

`gradle/libs.versions.toml` を作成する。`example/libs.versions.toml` をベースに、以下の依存を含める:

| ライブラリ | 用途 |
|---|---|
| `kotlin-compiler-embeddable` | Compiler API (compileOnly) |
| `auto-service-annotations` | META-INF/services 自動生成 |
| `auto-service-ksp` | AutoService の KSP プロセッサ |
| `kctfork-core` | KotlinCompilation テスト |
| `kotest-runner-junit5` | テストランナー |
| `kotest-assertions-core` | テストアサーション |

### Step 3: buildSrc Convention Plugins の作成

`example/buildSrc/` 内のファイルをコピーし、プロジェクト固有の値を置換する。

生成されるファイル:
- `buildSrc/build.gradle.kts` — kotlin-dsl プラグイン、Kotlin Gradle Plugin 依存
- `buildSrc/settings.gradle.kts` — 親の version catalog を再利用
- `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts` — JVM toolchain、JUnit5、テストログ設定

publish-convention が必要な場合は references/publish-convention.md を参照。

### Step 4: compiler-plugin モジュールの作成

`example/compiler-plugin/build.gradle.kts` をベースに作成する。

重要な設定:
- `kotlin-compiler-embeddable` は **compileOnly** (コンパイラ本体が提供)
- `auto-service-annotations` + `auto-service-ksp` で META-INF/services を自動生成
- `optIn` で `ExperimentalCompilerApi` と `UnsafeDuringIrConstructionAPI` を有効化

#### Plugin 登録ファイルの作成

references/plugin-registration.md を参照して以下を作成:

1. **CommandLineProcessor** — Plugin ID を宣言。`@AutoService(CommandLineProcessor::class)` で自動登録
2. **CompilerPluginRegistrar** — FIR / IR extension を登録。`supportsK2 = true` を設定

### Step 5: runtime モジュールの作成

`example/runtime/build.gradle.kts` をベースに作成。compiler plugin が提供する API の宣言をここに配置する。

- Kotlin Multiplatform で構成 (JVM, JS, Wasm, Native ターゲット)
- 関数シグネチャのみ宣言し、実装は compiler plugin が IR 変換で差し替える

### Step 6: gradle-plugin モジュールの作成

`example/gradle-plugin/build.gradle.kts` をベースに作成。

重要な設定:
- `java-gradle-plugin` プラグインを適用
- `gradlePlugin { plugins { create(...) } }` でプラグイン ID と実装クラスを登録
- compiler-plugin と runtime への依存を設定
- `KotlinCompilerPluginSupportPlugin` を実装して compiler plugin artifact を提供

references/gradle-plugin-impl.md にGradle plugin の実装パターンを記載。

### Step 7: Unit Test のセットアップ

`compiler-plugin/src/test/` に kctfork (KotlinCompilation) を使ったテストを作成する。

references/testing-patterns.md を参照して以下のヘルパーを用意:

```kotlin
fun compile(source: String): JvmCompilationResult =
    KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin("Source.kt", source))
        compilerPluginRegistrars = listOf(<YourPluginRegistrar>())
        inheritClassPath = true
        jvmTarget = "<java-version>"
        messageOutputStream = System.out
    }.compile()
```

テストカテゴリ:
1. **正常系** — 変換が正しく適用されるケース
2. **エラー系** — コンパイルエラーが期待されるケース (`ExitCode.COMPILATION_ERROR`)
3. **エッジケース** — 型バリエーション、ネスト、複数パラメータ等

### Step 8: Integration Test のセットアップ

2 種類の integration test モジュールを用意する:

#### test-jvm (JVM 単体)

`example/integration-test/test-jvm/build.gradle.kts` をベースに作成。

- `kotlin-jvm` + `application` プラグインで構成
- `kotlinCompilerPluginClasspath(project(":compiler-plugin"))` で compiler plugin を直接指定
- `application { mainClass = ... }` で `main()` 関数を実行可能にする

#### test-kmp (Kotlin Multiplatform)

`example/integration-test/test-kmp/build.gradle.kts` をベースに作成。

- `kotlin("multiplatform")` で構成 (JVM + JS ターゲット)
- `kotlinCompilerPluginClasspath(project(":compiler-plugin"))` で全ターゲットに compiler plugin を適用
- commonMain に runtime 依存を配置し、各ターゲットで動作確認

両モジュールとも `check()` や `assert()` で実行時に値を検証する。

### Step 9: ビルド確認

```bash
./gradlew jvmTest
./gradlew :integration-test:test-jvm:run
./gradlew :integration-test:test-kmp:jvmRun
```

## セットアップ完了メッセージ

```
## セットアップ完了

### プロジェクト構成
- buildSrc/ (convention plugins)
- compiler-plugin/ (FIR + IR extensions)
- gradle-plugin/ (KotlinCompilerPluginSupportPlugin)
- runtime/ (Multiplatform API declarations)
- integration-test/test-jvm/ (JVM E2E test)
- integration-test/test-kmp/ (KMP E2E test)

### 依存関係
- kotlin-compiler-embeddable: <version>
- auto-service + KSP
- kctfork: <version>
- kotest: <version>

### ビルド結果
- jvmTest: [SUCCESS / FAILED]
- integration-test: [SUCCESS / FAILED]

### 次のステップ
1. compiler-plugin/src/main/kotlin/ に FIR checker と IR transformer を実装
2. runtime/src/commonMain/kotlin/ に公開 API を宣言
3. compiler-plugin/src/test/ にテストケースを追加
```
