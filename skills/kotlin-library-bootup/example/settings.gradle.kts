pluginManagement {
    // convention plugin (example-lib.*) を提供する included build
    includeBuild("build-logic")
    repositories {
        // @bootup:if kmp
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("android.*")
            }
        }
        // @bootup:end
        gradlePluginPortal()
        mavenCentral()
    }
}

// jvmToolchain(17) 用の JDK を自動プロビジョニングする。
// ローカルに JDK 17 が無い環境でも "No matching toolchains" で落ちなくなる。
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        // @bootup:if kmp
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("android.*")
            }
        }
        // @bootup:end
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "example-lib"

// モジュールを増やしたら include を足し、root build.gradle.kts の dokka(project(...)) にも追加する
include(":example-lib-core")
