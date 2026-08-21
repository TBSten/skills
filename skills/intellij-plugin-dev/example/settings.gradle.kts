// 独立ビルド (references/setup/basics.md):
// 1 つの Gradle ビルド内で Kotlin Gradle Plugin を複数版併用できず、プラグインは
// 「ターゲット IDE 同梱の Kotlin 以下」でビルドせねばならないため、本体リポジトリの
// サブプロジェクトにせず自前 settings を持つ。task は必ずこのディレクトリをカレントにして叩く
// (root からは ./gradlew -p <plugin-module> ...)。

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // NOTE: version はここに書き、build.gradle.kts 側の id("org.jetbrains.intellij.platform") は
    // version 無指定にする (両方に書くと classpath 衝突 — setup/basics.md)。
    // 値は gradle/libs.versions.toml の intellijPlatformGradlePlugin と一致させる
    // (settings の plugins block からは version catalog を参照できない)。
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

rootProject.name = "example-plugin"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        // IntelliJ Platform SDK / bundled plugin / intellij-dependencies (icons 等) の解決
        intellijPlatform {
            defaultRepositories()
        }
    }
}
