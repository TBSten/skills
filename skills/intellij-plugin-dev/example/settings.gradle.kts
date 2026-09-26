// An independent build, not a subproject of the surrounding repository: one Gradle build cannot mix
// two versions of the Kotlin Gradle Plugin, and a plugin must be compiled with a Kotlin no newer
// than the one bundled in its target IDE. Run every task from this directory
// (or `./gradlew -p <this-directory> ...` from the repository root).

// Without this import `intellijPlatform { defaultRepositories() }` below does not resolve.
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // The version is written here only; build.gradle.kts applies the plugin without one, as
    // writing it in both places makes the classpaths collide. Keep it equal to
    // `intellijPlatformGradlePlugin` in gradle/libs.versions.toml, which this block cannot read.
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

rootProject.name = "example-plugin"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        // The IntelliJ Platform SDK, its bundled plugins and intellij-dependencies (icons etc.).
        intellijPlatform {
            defaultRepositories()
        }
    }
}
