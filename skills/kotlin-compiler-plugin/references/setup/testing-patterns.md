# Testing Patterns for Kotlin Compiler Plugins

## Unit Test with kctfork (KotlinCompilation)

kctfork は Kotlin ソースをインメモリでコンパイルし、結果を検証するライブラリ。

**compile ヘルパー (`compile` / `shouldCompileOk` / `loadTopLevelField`) と正常系テストの完全なコードは
`example/compiler-plugin/src/test/kotlin/com/example/compilerpluginsetup/ExampleCompilerTest.kt` が SSoT**
(scaffold.sh がコピー・置換する)。

### ヘルパーの設計

| ヘルパー | 役割 |
|---|---|
| `compile(source, dumpIr)` | `compilerPluginRegistrars` に自プラグインを登録してインメモリコンパイル |
| `shouldCompileOk()` | `ExitCode.OK` を検証、失敗時は `messages` 込みで AssertionError |
| `loadTopLevelField(name, pkg)` | 生成クラスをクラスローダーでロードし、トップレベルプロパティの値をリフレクション取得 |

テストカテゴリ:
1. **正常系** — 変換が正しく適用されるケース
2. **エラー系** — コンパイルエラーが期待されるケース (`ExitCode.COMPILATION_ERROR`)
3. **エッジケース** — 型バリエーション、ネスト、複数パラメータ等

### エラー系テスト

コンパイルエラーの検証:

```kotlin
test("non-existent function causes compile error") {
    val result = compile("""
        import <your-package>.runtime.<yourFunction>
        val v = <yourFunction><String>("nonExistent", "x")
    """.trimIndent())

    result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    result.messages shouldContain "Function 'nonExistent' not found"
}
```

### 複数アサーションの assertSoftly

関連する複数アサーションをまとめて検証:

```kotlin
test("multiple parameters") {
    val result = compile("""...""").shouldCompileOk()

    assertSoftly {
        result.loadTopLevelField("v1", pkg = "com.example.test") shouldBe "hello"
        result.loadTopLevelField("v2", pkg = "com.example.test") shouldBe 42
    }
}
```

### IR ダンプによるデバッグ

変換結果の IR を確認したい場合、`compile(source, dumpIr = true)` を使う
(`-Xphases-to-dump-after=IrVerification` が付与され、stdout に IR がダンプされる)。

## Integration Test

実際の Gradle プロジェクトとして compiler plugin を適用し、エンドツーエンドで検証する。
**build ファイルと Main.kt の完全なコードは example の実ファイルが SSoT**:

| モジュール | 実ファイル |
|---|---|
| test-jvm (JVM 単体) | `example/integration-test/test-jvm/build.gradle.kts` + `src/main/kotlin/.../testapp/Main.kt` |
| test-kmp (KMP: JVM + JS) | `example/integration-test/test-kmp/build.gradle.kts` + `src/commonMain/kotlin/.../testapp/Main.kt` |

### 設計ポイント

- **test-jvm**: `kotlin-jvm` convention + `application` プラグイン。`kotlinCompilerPluginClasspath(project(":compiler-plugin"))` で compiler plugin を直接適用し、`application { mainClass = ... }` で実行可能にする
- **test-kmp**: `kotlin("multiplatform")` (JVM + JS)。`kotlinCompilerPluginClasspath` は全ターゲットに適用される。commonMain に runtime 依存とテストコードを配置し、各ターゲットで変換を確認する
- **Main.kt**: `check()` で実行時に値を検証する。`check()` 失敗はプロセスの非ゼロ終了になるため CI でも検出可能

### 実行方法

```bash
# JVM 単体
./gradlew :integration-test:test-jvm:run

# KMP (JVM ターゲット)
./gradlew :integration-test:test-kmp:jvmRun
```

## Gradle plugin のテスト (2 層)

`kotlinCompilerPluginClasspath` 直指定の integration test は Gradle plugin を通らない。Gradle plugin は次の 2 層で検証する:

| 層 | モジュール | 手段 | 検証範囲 | 速度 |
|---|---|---|---|---|
| sanity | `:gradle-plugin:test` | `ProjectBuilder` + `(project as ProjectInternal).evaluate()` | DSL 登録・`convention` 既定値・runtime 依存の追加先 (JVM / KMP)・版比較 | 秒 |
| E2E | `:integration-test:test-gradle-plugin:test` | Gradle TestKit (`GradleRunner`) + fixture | `plugins { id(...) }` での解決・compiler plugin の attach・runtime 依存・実コンパイル/実行 | 1 build 30〜90 秒 |

### TestKit fixture の構成

実ファイル: `example/integration-test/test-gradle-plugin/` (`build.gradle.kts` / `src/test/kotlin/.../ExampleGradlePluginE2eTest.kt` / `src/test/resources/fixtures/jvm-sample/`)

- fixture は **root build に include しない独立 Gradle プロジェクト**。テストが temp dir にコピーしてから `GradleRunner.withProjectDir(...)` で起動する (src/test/resources に `build/` や `.gradle/` を作らない)
- 本リポジトリのパス・Kotlin 版は `-Pe2e.rootBuildDir=` / `-Pe2e.kotlinVersion=` で渡す (test task の systemProperty 経由)。fixture の settings は `settings.providers.gradleProperty(...)` で読む
- `pluginManagement { includeBuild(root) }` は plugin 解決用。通常依存 (`SubpluginArtifact` の `GROUP:compiler-plugin`、自動追加される `GROUP:runtime`) は **top-level の `includeBuild(root) { dependencySubstitution { ... } }` も必要**。KMP runtime は `runtime` と `runtime-jvm` の両方を substitute する
- publish (mavenLocal / Maven Central) せずに未公開の成果物で利用者と同じ書き方を検証できる
- fixture に `gradle.properties` で `-Xmx1g -XX:MaxMetaspaceSize=768m` を与える。TestKit の子 daemon は既定 (heap 512MiB / metaspace 384MiB) だと Kotlin compiler + plugin のロードで `OutOfMemoryError: Metaspace` になり、毎回違うテストが落ちる flaky になる
- `--no-configuration-cache` を付ける (includeBuild + substitution と相性が悪い場合がある)
- 検証したい配線 (plugin classpath / runtime classpath) は fixture 側の小さなタスクで stdout に出して assert する
- 複数 Kotlin 版で回す場合は fixture の Kotlin 版を `-Pe2e.kotlinVersion` で差し替える

KMP fixture (`kotlin("multiplatform")` + `commonMainImplementation` 追加の確認) が必要になったら `fixtures/kmp-sample/` を同じ形で追加する。
