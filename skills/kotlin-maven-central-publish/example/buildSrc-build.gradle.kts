plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Add a dependency on the Kotlin Gradle plugin, so that convention plugins can apply it.
    implementation(libs.kotlinGradlePlugin)
    // Vanniktech Maven Publish plugin for convention plugin usage.
    // This pattern converts a plugin ID to a dependency coordinate.
    implementation(libs.plugins.mavenPublish.map {
        "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
    })
}
