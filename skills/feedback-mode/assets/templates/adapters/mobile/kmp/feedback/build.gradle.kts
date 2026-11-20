import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :feedback モジュール（フィードバックモードの本体）。src/commonMain/kotlin/ には compose/ の 5 枚と PlatformFeedback.kt を置く。
// 依存させるのはフィードバックを入れるビルドだけ（androidApp は debugImplementation、iOS / Desktop / Web はスイッチ。kmp.md）
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.example.app.feedback" // TODO: 導入先に合わせる
        compileSdk = 37
        minSdk = 23
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    jvm { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
