package com.example.compilerpluginsetup.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder

/**
 * Fast sanity tests with ProjectBuilder (DSL wiring only, no real compilation).
 * The real Gradle build E2E is `:integration-test:test-gradle-plugin` (TestKit).
 */
class ExampleGradlePluginTest : FunSpec({

    fun projectWith(vararg pluginIds: String): Project =
        ProjectBuilder.builder().build().also { project ->
            pluginIds.forEach { project.pluginManager.apply(it) }
        }

    fun Project.evaluate() = (this as ProjectInternal).evaluate()

    fun Project.dependencyNotations(configName: String): List<String> =
        configurations.getByName(configName).dependencies.map { "${it.group}:${it.name}:${it.version}" }

    val runtimeNotation = "${BuildConfig.GROUP_ID}:runtime:${BuildConfig.VERSION}"

    test("DSL extension が登録され、既定値は convention で埋まる") {
        val project = projectWith("org.jetbrains.kotlin.jvm", BuildConfig.COMPILER_PLUGIN_ID)

        val extension = project.extensions.findByType(ExampleExtension::class.java)
        extension shouldNotBe null
        extension!!.enabled.get().shouldBeTrue()
        extension.addRuntimeDependency.get().shouldBeTrue()
    }

    test("JVM project では implementation に runtime 依存が追加される") {
        val project = projectWith("org.jetbrains.kotlin.jvm", BuildConfig.COMPILER_PLUGIN_ID)
        project.evaluate()

        project.dependencyNotations("implementation") shouldContain runtimeNotation
    }

    test("KMP project では commonMainImplementation に runtime 依存が追加される") {
        val project = projectWith("org.jetbrains.kotlin.multiplatform", BuildConfig.COMPILER_PLUGIN_ID)
        project.evaluate()

        project.dependencyNotations("commonMainImplementation") shouldContain runtimeNotation
    }

    test("addRuntimeDependency = false なら runtime 依存を追加しない") {
        val project = projectWith("org.jetbrains.kotlin.jvm", BuildConfig.COMPILER_PLUGIN_ID)
        project.extensions.getByType(ExampleExtension::class.java).addRuntimeDependency.set(false)
        project.evaluate()

        project.dependencyNotations("implementation").contains(runtimeNotation) shouldBe false
    }

    test("Kotlin バージョン比較は pre-release を同じ base の stable より小さく扱う") {
        val rc = KotlinVersionParts.parse("2.4.0-RC")!!
        val stable = KotlinVersionParts.parse("2.4.0")!!

        (rc < stable).shouldBeTrue()
        (KotlinVersionParts.parse("2.3.10")!! < rc).shouldBeTrue()
        KotlinVersionParts.parse("snapshot") shouldBe null
    }
})
