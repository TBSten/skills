package com.example.compilerpluginsetup.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

/**
 * Gradle TestKit E2E: runs a consumer build (`fixtures/<name>/`) that applies the plugin with
 * `plugins { id("com.example.compilerpluginsetup") }` and resolves everything from this
 * repository via includeBuild + dependencySubstitution (see the fixture's settings.gradle.kts).
 */
class ExampleGradlePluginE2eTest : FunSpec({

    fun sysProp(name: String): String =
        requireNotNull(System.getProperty(name)) { "System property '$name' is not set (run via Gradle)" }

    val fixturesDir = File(sysProp("e2e.fixturesDir"))

    /** Copies the fixture to a temp dir so the build never writes into src/test/resources. */
    fun fixture(name: String): File =
        tempdir().also { dir -> File(fixturesDir, name).copyRecursively(dir, overwrite = true) }

    fun runner(projectDir: File, vararg tasks: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                *tasks,
                "-Pe2e.rootBuildDir=${sysProp("e2e.rootBuildDir")}",
                "-Pe2e.kotlinVersion=${sysProp("e2e.kotlinVersion")}",
                "--stacktrace",
                // includeBuild + dependencySubstitution fixtures are simpler without configuration cache.
                "--no-configuration-cache",
            )
            .forwardOutput()

    test("plugin id で適用すると compiler plugin と runtime が配線される") {
        val result = runner(fixture("jvm-sample"), "printE2eWiring").build()

        result.output shouldContain Regex("E2E_PLUGIN_CLASSPATH=.*compiler-plugin")
        result.output shouldContain Regex("E2E_RUNTIME_CLASSPATH=.*runtime")
    }

    test("compiler plugin を適用した状態でコンパイル・実行できる") {
        val result = runner(fixture("jvm-sample"), "run").build()

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "E2E_RESULT=ok"
    }
})
