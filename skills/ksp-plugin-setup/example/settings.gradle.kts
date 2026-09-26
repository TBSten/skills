pluginManagement {
    // buildLogic provides the `buildLogic.*` convention plugins. Including it from pluginManagement
    // (not the top level) makes it a plugin build: modules apply its plugins by id, and it is
    // configured before the main build so the conventions can share the root version catalog.
    includeBuild("buildLogic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Auto-provision the JDK 17 toolchain (`jvmToolchain(17)`) for contributors who do not have a
// matching JDK installed locally, so the toolchain spec never fails with "No matching toolchains".
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "<project-name>"

include(":<project-name>-runtime")
include(":<project-name>-ksp")
include(":test")
