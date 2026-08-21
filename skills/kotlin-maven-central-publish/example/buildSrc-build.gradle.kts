plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Vanniktech Maven Publish plugin for convention plugin usage.
    // This pattern converts a plugin ID to a dependency coordinate.
    implementation(libs.plugins.mavenPublish.map {
        "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
    })
}
