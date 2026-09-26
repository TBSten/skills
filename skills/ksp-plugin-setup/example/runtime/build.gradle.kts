import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The runtime module declares annotations ONLY — zero runtime logic — so it can target every
// Kotlin platform. Anything with behaviour belongs in the ksp module (JVM only) instead.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // AGP 9: the Android target of a KMP library comes from this plugin, configured inside
    // `kotlin { android { } }` (the old `com.android.library` + `androidTarget()` pair is gone).
    alias(libs.plugins.androidKmpLibrary)
    id("buildLogic.publish")
    id("buildLogic.lint")
}

description = "<one-line description>"

kotlin {
    explicitApi()
    jvmToolchain(17)

    // tested on CI
    iosSimulatorArm64()
    jvm()
    linuxX64()
    android {
        namespace = "com.example.ksppluginsetup"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    // not tested on CI (x86_64 Apple targets are omitted: deprecated by Kotlin, add them back only if you must)
    macosArm64()
    iosArm64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    tvosSimulatorArm64()
    tvosArm64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }
}
