import java.time.Duration

plugins {
    id("buildsrc.convention.kotlin-jvm")
}

// ---------------------------------------------------------------------------
// Real-build E2E of the Gradle plugin with Gradle TestKit.
//
// Each fixture under src/test/resources/fixtures/<name>/ is an independent Gradle build.
// The test copies it to a temp dir and runs it with GradleRunner. The fixture's settings
// `includeBuild(<this repo>)` + dependencySubstitution resolve the plugin, the compiler plugin
// and the runtime from this repository's unpublished sources — exactly what a consumer writes
// (`plugins { id("<plugin-id>") }`), without publishing to Maven Central / mavenLocal.
//
//   :gradle-plugin:test                        = ProjectBuilder sanity (fast, DSL wiring)
//   :integration-test:test-gradle-plugin:test  = TestKit fixture (slow, real consumer build)
// ---------------------------------------------------------------------------

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestAssertionsCore)
}

tasks.withType<Test>().configureEach {
    val rootBuildDir = rootProject.layout.projectDirectory.asFile.absolutePath
    val kotlinVersion = libs.versions.kotlin.get()
    systemProperty("e2e.fixturesDir", layout.projectDirectory.dir("src/test/resources/fixtures").asFile.absolutePath)
    systemProperty("e2e.rootBuildDir", rootBuildDir)
    systemProperty("e2e.kotlinVersion", kotlinVersion)
    // Each TestKit build takes 30-90 s.
    timeout.set(Duration.ofMinutes(15))
}
