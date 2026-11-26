// TestKit fixture: an independent consumer build. Values are passed by the test with -P:
//   e2e.rootBuildDir  = absolute path of the plugin repository (included build)
//   e2e.kotlinVersion = Kotlin version of the plugin repository (libs.versions.kotlin)
pluginManagement {
    // Resolves `plugins { id("<plugin-id>") }` from the included build's :gradle-plugin.
    includeBuild(settings.providers.gradleProperty("e2e.rootBuildDir").get())
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version settings.providers.gradleProperty("e2e.kotlinVersion").get()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// pluginManagement's includeBuild only covers plugin resolution. Include the same build again
// at the top level to substitute the regular dependencies the Gradle plugin adds
// (`<group>:compiler-plugin` via SubpluginArtifact, `<group>:runtime` via addRuntimeDependency).
includeBuild(settings.providers.gradleProperty("e2e.rootBuildDir").get()) {
    dependencySubstitution {
        substitute(module("com.example.compilerpluginsetup:compiler-plugin")).using(project(":compiler-plugin"))
        substitute(module("com.example.compilerpluginsetup:runtime")).using(project(":runtime"))
        substitute(module("com.example.compilerpluginsetup:runtime-jvm")).using(project(":runtime"))
    }
}

rootProject.name = "jvm-sample"
