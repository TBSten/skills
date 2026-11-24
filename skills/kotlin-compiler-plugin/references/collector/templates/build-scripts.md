# ビルドスクリプト テンプレート

## テンプレート変数

- `{{GROUP_ID}}` — Maven groupId (例: `com.example`)
- `{{ARTIFACT_PREFIX}}` — artifactId の prefix (例: `collector`)
- `{{VERSION}}` — バージョン (例: `0.1.0`)
- `{{PACKAGE}}` — Kotlin パッケージ名 (例: `com.example.collector`)
- `{{PLUGIN_ID}}` — Gradle Plugin ID (例: `com.example.collector`)
- `{{KOTLIN_VERSION}}` — Kotlin バージョン (2.3.x+)

## settings.gradle.kts

```kotlin
rootProject.name = "{{ARTIFACT_PREFIX}}"

include(":annotations")
include(":compiler-plugin")
include(":gradle-plugin")
```

## root build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "{{KOTLIN_VERSION}}" apply false
    kotlin("multiplatform") version "{{KOTLIN_VERSION}}" apply false
}

allprojects {
    group = "{{GROUP_ID}}"
    version = "{{VERSION}}"
}
```

## annotations/build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

kotlin {
    jvm()
    // 必要に応じて他のターゲットを追加
    // iosArm64()
    // iosSimulatorArm64()
    // js(IR) { browser(); nodejs() }

    sourceSets {
        commonMain {
            dependencies {
                // runtime 依存なし (アノテーションのみ)
            }
        }
    }
}

publishing {
    publications {
        // KMP は自動で publication を作成する
    }
}
```

## compiler-plugin/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    // Kotlin Compiler Plugin API
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:{{KOTLIN_VERSION}}")

    // テスト
    testImplementation("dev.zacsweers.kctfork:core:0.7.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:{{KOTLIN_VERSION}}")
    testImplementation(project(":annotations"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "{{ARTIFACT_PREFIX}}-compiler-plugin"
            from(components["java"])
        }
    }
}
```

## gradle-plugin/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("maven-publish")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:{{KOTLIN_VERSION}}")
}

gradlePlugin {
    plugins {
        create("collector") {
            id = "{{PLUGIN_ID}}"
            implementationClass = "{{PACKAGE}}.gradle.CollectorGradlePlugin"
        }
    }
}
```

## META-INF ファイル

### compiler-plugin/src/main/resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar

```
{{PACKAGE}}.compiler.CollectorPluginRegistrar
```

### gradle-plugin/src/main/resources/META-INF/gradle-plugins/{{PLUGIN_ID}}.properties

```
implementation-class={{PACKAGE}}.gradle.CollectorGradlePlugin
```
