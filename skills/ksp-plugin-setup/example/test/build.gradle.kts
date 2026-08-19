@file:OptIn(KspExperimental::class)

import com.google.devtools.ksp.KspExperimental

// The `test` module applies the processor for real and verifies the RUNTIME BEHAVIOUR of the
// generated declarations on every target — the complement to the JVM-only kctfork suite.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    // kotest must be applied AFTER ksp: its multiplatform framework wiring is itself KSP-based.
    alias(libs.plugins.kotest)
    id("buildLogic.lint")
}

android {
    namespace = "com.example.ksppluginsetup.test"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)

    iosSimulatorArm64()
    jvm()
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":<project-name>-runtime"))
        }
        commonTest.dependencies {
            implementation(libs.kotest)
            implementation(libs.kotestFrameworkEngine)
        }
        jvmTest.dependencies {
            implementation(libs.kotestRunnerJunit5)
        }
        // androidUnitTest does NOT inherit jvmTest, so it needs the kotest JUnit5 runner of its own
        // to discover specs.
        androidUnitTest.dependencies {
            implementation(libs.kotestRunnerJunit5)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // Only commonMain annotations are processed (collapsed into kspCommonMainKotlinMetadata by the
    // workaround below). Deliberately NOT wired into any *Test KSP configuration so the kotest KSP
    // processor owns the test source sets without a second processor running alongside it.
    listOf(
        "kspCommonMainMetadata",
        "kspJvm",
    ).forEach { it(project(":<project-name>-ksp")) }
}

// ksp {
//     arg("<project-name>.greetingFunPrefix", "greet")
//     arg("<project-name>.greetingStyle", "polite")
// }

/**
 * KSP does not support intermediate source sets such as `commonMain`. Generate once into the
 * metadata source set, add that output to commonMain, and make the per-platform tasks depend on it.
 */
fun Project.setupKspForMultiplatformWorkaround() {
    kotlin.sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }

    tasks.configureEach {
        if (name.startsWith("ksp") && name != "kspCommonMainKotlinMetadata") {
            dependsOn(tasks.named("kspCommonMainKotlinMetadata"))
            // Disable only the redundant per-platform *main* generation. Keep the *Test KSP tasks
            // alive so the kotest framework can generate its per-target spec launchers (required for
            // specs to start on native/Android, harmless on JVM).
            if (!name.contains("Test")) {
                enabled = false
            }
        }
    }
}
setupKspForMultiplatformWorkaround()

/** ktlint would otherwise race the generator over commonMain, and then lint the generated output. */
fun ktlintWithKspWorkaround() {
    tasks.named("runKtlintFormatOverCommonMainSourceSet") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
    tasks.named("runKtlintCheckOverCommonMainSourceSet") {
        dependsOn("kspCommonMainKotlinMetadata")
    }

    ktlint {
        filter {
            exclude("**/build/generated/**")
        }
    }
}
ktlintWithKspWorkaround()
