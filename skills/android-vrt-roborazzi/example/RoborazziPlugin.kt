package com.example.androidvrtroborazzi

import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class RoborazziPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins.apply("com.github.takahirom.roborazzi")
            
            // Note: Requires Robolectric setup
            // plugins.apply("org.robolectric.gradle")

            android {
                testOptions {
                    unitTests {
                        isIncludeAndroidResources = true
                        all {
                            it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
                        }
                    }
                }
            }

            dependencies {
                add("testImplementation", "io.github.takahirom.roborazzi:roborazzi:1.40.1")
                add("testImplementation", "io.github.takahirom.roborazzi:roborazzi-compose:1.40.1")
                add("testImplementation", "io.github.takahirom.roborazzi:roborazzi-junit-rule:1.40.1")
                add("testImplementation", "io.github.takahirom.roborazzi:roborazzi-compose-preview-scanner-support:1.40.1")
                add("testImplementation", "com.github.sergio-sastre.ComposablePreviewScanner:android:0.3.0")
            }

            val projectPath = target.path.split(":").filter { it.isNotEmpty() }.joinToString("/")
            val outputDirectory = rootProject.layout.buildDirectory.dir("roborazzi-outputs").get().dir(projectPath)

            extensions.configure<RoborazziExtension> {
                outputDir.set(outputDirectory)
                compare.outputDir.set(outputDirectory)
            }

            tasks.findByName("clean")?.doLast {
                outputDirectory.asFile.deleteRecursively()
            }

            configureComposePreviewTests()
        }
    }
}

private fun Project.android(action: com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>.() -> Unit) {
    extensions.configure("android", action)
}

private fun Project.configureComposePreviewTests() {
    val roborazzi = extensions.getByType(RoborazziExtension::class.java)
    roborazzi.generateComposePreviewRobolectricTests {
        enable = true
        // packages = provider { listOf(android.namespace) }
        robolectricConfig = mapOf(
            "sdk" to "[32]",
            "qualifiers" to "RobolectricDeviceQualifiers.Pixel5",
        )
        includePrivatePreviews = true
        testerQualifiedClassName = "com.example.androidvrtroborazzi.AppComposePreviewTester"
    }
}
