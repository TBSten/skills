// convention plugin (precompiled script plugin) を提供する included build。
// ルートの settings.gradle.kts から pluginManagement { includeBuild("build-logic") } で取り込む。
pluginManagement {
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

// build-logic 自身のビルドにも JDK 自動プロビジョニングを効かせる (ルート settings と対で設定する)
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
        // plugin marker (<id>:<id>.gradle.plugin) の解決に必要
        gradlePluginPortal()
        mavenCentral()
    }
    // 本体と同じ version catalog を共有し、plugin バージョンの SSoT を gradle/libs.versions.toml に保つ
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
