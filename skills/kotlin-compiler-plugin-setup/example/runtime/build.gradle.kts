plugins {
    kotlin("multiplatform")
}

kotlin {
    // JVM
    jvm()
    // JS
    js { browser(); nodejs() }
    // Wasm
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { nodejs() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi { nodejs() }
    // Native - Tier 1
    linuxX64()
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    // Native - Tier 2
    linuxArm64()
    iosArm64()
    // Add more targets as needed
}
