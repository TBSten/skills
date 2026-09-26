plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Put Gradle plugins on the convention build's classpath as plugin markers
    // (<id>:<id>.gradle.plugin:<version>). Versions stay in the version catalog (SSoT).
    //
    // The Kotlin (and Android) Gradle plugin must live on the SAME classpath as vanniktech:
    // vanniktech 0.36+ touches KGP classes and fails with "was not able to access Kotlin plugin classes"
    // otherwise. In buildSrc, modules must then request those plugins WITHOUT a version
    // (e.g. id(libs.plugins.kotlin.jvm.get().pluginId) instead of alias(...)), or Gradle fails with
    // "already on the classpath with an unknown version". In an included build (build-logic),
    // keep alias(...) with the version as is.
    // setup-publish.sh fills in the catalog accessors below.
<PLUGIN_DEPENDENCIES>
}

fun plugin(plugin: Provider<PluginDependency>): Provider<String> =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
