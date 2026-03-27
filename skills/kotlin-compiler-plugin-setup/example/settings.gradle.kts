pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":compiler-plugin")
include(":runtime")
include(":gradle-plugin")
include(":integration-test:test-jvm")
include(":integration-test:test-kmp")

rootProject.name = "<project-name>"
