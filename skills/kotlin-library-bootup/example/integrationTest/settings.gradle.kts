// integrationTest はライブラリ本体とは独立した Gradle ビルド。
// 利用者と同じく Maven 座標 (<group-id>:example-lib-core) でライブラリに依存し、
// includeBuild("../") によってローカルのプロジェクトへ置き換えて検証する。
//
// 実行: ./gradlew -p integrationTest test   (ルートの wrapper をそのまま使える)
// 置換されているかの確認:
//   ./gradlew -p integrationTest :consumer:dependencyInsight --configuration testRuntimeClasspath --dependency example-lib-core
//   -> "(by composite build)" が出れば OK

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

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
    versionCatalogs {
        // ルートの catalog を共有し、テスト対象ライブラリとバージョンがずれないようにする
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// include するビルドのルートプロジェクト名 (example-lib) と被らない名前にする
rootProject.name = "example-lib-integration-test"

includeBuild("../")

include(":consumer")
