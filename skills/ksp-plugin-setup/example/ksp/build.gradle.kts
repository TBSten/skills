// The processor module is JVM only (a KSP limitation) and depends on the runtime module for the
// annotation declarations it looks up.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.vanniktech.mavenPublish)
    id("buildLogic.lint")
}

kotlin {
    explicitApi()
    jvmToolchain(17)

    compilerOptions.optIn.addAll(
        "com.google.devtools.ksp.KspExperimental",
    )
    // Layered `context(...)` parameters (ProcessContext for feature, narrowed capabilities for core)
    // still need the opt-in flag on Kotlin 2.2.x.
    compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
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
    System.getProperty("<project-name>.snapshot.update")?.let {
        systemProperty("<project-name>.snapshot.update", it)
    }
}

mavenPublishing {
    publishToMavenCentral()

    if (!gradle.startParameter.taskNames.contains("publishToMavenLocal")) {
        signAllPublications()
    }

    coordinates(group.toString(), "<project-name>-ksp", version.toString())

    pom {
        name = "<project-name> ksp plugin"
        description = "<one-line description>"
        inceptionYear = "<year>"
        url = "https://github.com/<owner>/<repo>/"
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id = "<owner>"
                name = "<owner>"
                url = "https://github.com/<owner>/"
            }
        }
        scm {
            url.set("https://github.com/<owner>/<repo>/")
            connection.set("scm:git:git://github.com/<owner>/<repo>.git")
            developerConnection.set("scm:git:git://github.com/<owner>/<repo>.git")
        }
    }
}
