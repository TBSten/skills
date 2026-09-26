// The processor module is JVM only (a KSP limitation) and depends on the runtime module for the
// annotation declarations it looks up.
plugins {
    alias(libs.plugins.kotlinJvm)
    id("buildLogic.publish")
    id("buildLogic.lint")
}

description = "<one-line description>"

kotlin {
    explicitApi()
    jvmToolchain(17)

    compilerOptions.optIn.addAll(
        "com.google.devtools.ksp.KspExperimental",
    )
    // Layered `context(...)` parameters (ProcessContext for feature, narrowed capabilities for core)
    // are stable since Kotlin 2.4; on 2.2.x / 2.3.x add `-Xcontext-parameters` to freeCompilerArgs.
    sourceSets.named("test") {
        languageSettings.optIn("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

dependencies {
    implementation(project(":<project-name>-runtime"))
    implementation(libs.kspApi)
    implementation(kotlin("reflect"))

    testImplementation(libs.kotest)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestProperty)
    // kctfork drives the real Kotlin compiler + KSP in-process for e2e tests.
    testImplementation(libs.kctforkCore)
    testImplementation(libs.kctforkKsp)
    // Konsist enforces the feature/core/options/util layering from the test source set.
    testImplementation(libs.konsist)
    // KotlinPoet builds snapshot scenario inputs (test-only; generation itself uses string append).
    testImplementation(libs.kotlinPoet)
}

tasks.named<Test>("test") {
    // JVM-only module: kotest runs on the JUnit Platform, no io.kotest plugin needed.
    useJUnitPlatform()
    // Each kctfork test runs an in-process Kotlin/KSP compilation whose classloaders accumulate, so
    // the default worker heap is exhausted by a full suite (OutOfMemoryError cascading into
    // unrelated failures). Give the worker headroom and recycle it periodically.
    maxHeapSize = "2g"
    forkEvery = 25L
    // `-D` flags do NOT propagate to the test worker JVM automatically — forward explicitly.
    // `providers.systemProperty` (not System.getProperty) keeps the value a tracked input, so the
    // configuration cache is invalidated and the task re-runs when the flag changes.
    providers.systemProperty("<project-name>.snapshot.update").orNull?.let {
        systemProperty("<project-name>.snapshot.update", it)
    }
}
