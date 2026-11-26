plugins {
    kotlin("multiplatform")
    id("buildsrc.convention.publish")
}

kotlin {
    // Supporting consumers on an older Kotlin than this build (SUPPORTED_KOTLIN_MIN < kotlin)?
    // Pin apiVersion / languageVersion and keep kotlin-stdlib out of the published metadata:
    // see references/multi-version-setup.md "consumer 側 Kotlin の床".

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

mavenPublishing {
    pom {
        name.set("example-plugin runtime")
        description.set("Runtime API of example-plugin (Kotlin Multiplatform)")
    }
}
