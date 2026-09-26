plugins {
    `kotlin-dsl`
}

group = "<group-id>.buildLogic"

kotlin {
    // Compile against a provisioned JDK 17 (configures both the Java and Kotlin toolchains) so the
    // included build does not depend on the JDK that happens to launch Gradle. Provisioning is
    // enabled by the foojay resolver in buildLogic/settings.gradle.kts.
    jvmToolchain(17)
}

dependencies {
    // Put each Gradle plugin a convention applies on the classpath via its plugin MARKER, derived
    // from `[plugins]` in the catalog. The plugin is then declared once (as a plugin) instead of
    // twice (as a plugin AND as a `[libraries]` entry for its implementation artifact).
    implementation(plugin(libs.plugins.kotlinJvm))
    implementation(plugin(libs.plugins.ktlintGradle))
    implementation(plugin(libs.plugins.vanniktech.mavenPublish))
}

fun plugin(plugin: Provider<PluginDependency>): Provider<String> =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

gradlePlugin {
    plugins {
        register("lint") {
            id = "buildLogic.lint"
            implementationClass = "LintPlugin"
        }
        register("publish") {
            id = "buildLogic.publish"
            implementationClass = "PublishPlugin"
        }
    }
}
