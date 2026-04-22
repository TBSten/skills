package com.example.androidconventionplugins.primitive

import com.example.androidconventionplugins.dsl.libs
import com.example.androidconventionplugins.dsl.library
import com.example.androidconventionplugins.dsl.plugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.plugin("kotlinCompose").pluginId)

            // Configure Compose
            dependencies {
                val bom = platform(libs.library("composeBom"))
                add("implementation", bom)
                add("implementation", libs.library("composeUi"))
                add("implementation", libs.library("composeMaterial3"))
                add("debugImplementation", libs.library("composeUiTooling"))
            }
        }
    }
}
