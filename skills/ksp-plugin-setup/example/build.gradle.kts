plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.ksp) apply false
}

// The version catalog is the single source of truth for the project's own version too, so
// buildLogic and every module read the same value.
allprojects {
    group = "<group-id>"
    version = rootProject.libs.versions.<project-name>.get()
}
