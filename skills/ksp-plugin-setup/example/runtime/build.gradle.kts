import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The runtime module declares annotations ONLY — zero runtime logic — so it can target every
// Kotlin platform. Anything with behaviour belongs in the ksp module (JVM only) instead.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    id("buildLogic.lint")
}

kotlin {
    explicitApi()
    jvmToolchain(17)

    // tested on CI
    iosSimulatorArm64()
    jvm()
    linuxX64()
    androidTarget {
        publishLibraryVariants("release")
    }
    // not tested on CI
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()

    js(IR) {
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

android {
    namespace = "com.example.ksppluginsetup"
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

mavenPublishing {
    publishToMavenCentral()

    // Skip signing for local publishing so contributors do not need a GPG key to try the artifact.
    if (!gradle.startParameter.taskNames.contains("publishToMavenLocal")) {
        signAllPublications()
    }

    coordinates(group.toString(), "<project-name>-runtime", version.toString())

    pom {
        name = "<project-name> runtime"
        description = "<one-line description>"
        inceptionYear = "<year>"
        url = "https://github.com/<owner>/<repo>/"
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id = "<owner>"
                name = "<owner>"
                url = "https://github.com/<owner>/"
            }
        }
        scm {
            url.set("https://github.com/<owner>/<repo>/")
            connection.set("scm:git:git://github.com/<owner>/<repo>.git")
            developerConnection.set("scm:git:git://github.com/<owner>/<repo>.git")
        }
    }
}
