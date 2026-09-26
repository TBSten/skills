import kotlinx.validation.ExperimentalBCVApi

plugins {
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ksp) apply false
    // Guards the published API beyond `explicitApi()`: `apiCheck` (part of `check`) fails when the
    // committed `<module>/api/*.api` dumps no longer match. Refresh them with `./gradlew apiDump`.
    alias(libs.plugins.binaryCompatibilityValidator)
}

// The version catalog is the single source of truth for the project's own version too, so
// buildLogic and every module read the same value.
allprojects {
    group = "<group-id>"
    version = rootProject.libs.versions.<project-name>.get()
}

apiValidation {
    // The runtime module is KMP: validate the klib ABI too, not only the JVM class files.
    @OptIn(ExperimentalBCVApi::class)
    klib.enabled = true
    // Only published modules have an API to protect.
    ignoredProjects.add("test")
    // Declarations behind an internal opt-in marker are not public API. Uncomment once the runtime
    // declares one (see ksp-plugin-setup references/processor-design.md "Runtime API conventions").
    // nonPublicMarkers.add("com.example.ksppluginsetup.InternalExampleApi")
}
