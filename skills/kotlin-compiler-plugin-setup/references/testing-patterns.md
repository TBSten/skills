# Testing Patterns for Kotlin Compiler Plugins

## Unit Test with kctfork (KotlinCompilation)

kctfork は Kotlin ソースをインメモリでコンパイルし、結果を検証するライブラリ。

### 基本セットアップ

```kotlin
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class <YourPlugin>Test : FunSpec({

    fun compile(source: String, dumpIr: Boolean = false): JvmCompilationResult =
        KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Source.kt", source))
            compilerPluginRegistrars = listOf(<YourPlugin>Registrar())
            inheritClassPath = true
            jvmTarget = "21"
            messageOutputStream = System.out
            if (dumpIr) kotlincArguments = listOf("-Xphases-to-dump-after=IrVerification")
        }.compile()

    fun JvmCompilationResult.shouldCompileOk(): JvmCompilationResult {
        if (exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n$messages")
        }
        return this
    }

    fun JvmCompilationResult.loadTopLevelField(
        name: String,
        pkg: String? = null,
    ): Any? {
        val className = if (pkg != null) "$pkg.SourceKt" else "SourceKt"
        return classLoader.loadClass(className)
            .getDeclaredField(name)
            .also { it.isAccessible = true }
            .get(null)
    }
})
```

### 正常系テスト

コンパイル成功を検証し、リフレクションで変換結果の値を取得する:

```kotlin
test("default value is captured") {
    val result = compile("""
        package com.example.test
        import <your-package>.runtime.<yourFunction>
        fun target(x: String = "hello") {}
        val v = <yourFunction><String>(::target, "x")
    """.trimIndent())

    result.shouldCompileOk()
    result.loadTopLevelField("v", pkg = "com.example.test") shouldBe "hello"
}
```

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

### 複数テストの assertSoftly

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

変換結果の IR を確認したい場合、`dumpIr = true` を渡す:

```kotlin
test("debug IR output") {
    compile("""...""", dumpIr = true).shouldCompileOk()
    // stdout に IR ダンプが出力される
}
```

## Integration Test

実際の Gradle プロジェクトとして compiler plugin を適用し、エンドツーエンドで検証する。

### test-jvm (JVM 単体)

```kotlin
// integration-test/test-jvm/build.gradle.kts
plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    implementation(project(":runtime"))
    kotlinCompilerPluginClasspath(project(":compiler-plugin"))
}

application {
    mainClass = "<your-package>.testapp.MainKt"
}
```

### test-kmp (Kotlin Multiplatform)

```kotlin
// integration-test/test-kmp/build.gradle.kts
plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        mainRun {
            mainClass = "<your-package>.testapp.MainKt"
        }
    }
    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":runtime"))
        }
    }
}

dependencies {
    kotlinCompilerPluginClasspath(project(":compiler-plugin"))
}
```

KMP の integration test では commonMain にテストコードを配置し、各ターゲットで compiler plugin の変換が正しく適用されることを確認する。

### Main.kt

```kotlin
package <your-package>.testapp

fun main() {
    // compiler plugin の変換結果を実行時に検証
    val result = // ... compiler plugin が変換した値
    check(result == expectedValue) { "Expected $expectedValue, got $result" }
    println("OK: $result")
}
```

### 実行方法

```bash
# JVM 単体
./gradlew :integration-test:test-jvm:run

# KMP (JVM ターゲット)
./gradlew :integration-test:test-kmp:jvmRun
```

`check()` が失敗するとプロセスが非ゼロ終了コードで終了するため、CI でも検出可能。
