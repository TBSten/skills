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
