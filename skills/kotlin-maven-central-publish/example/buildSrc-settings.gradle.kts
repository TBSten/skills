// buildSrc/settings.gradle.kts として配置する。
// buildSrc はルートプロジェクトの version catalog をデフォルトでは参照できないため、
// ここで import しないと buildSrc/build.gradle.kts の `libs.plugins.mavenPublish` が解決できない。
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
