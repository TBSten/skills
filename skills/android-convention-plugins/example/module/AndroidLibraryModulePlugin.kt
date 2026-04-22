package com.example.androidconventionplugins.module

import com.example.androidconventionplugins.dsl.alias
import com.example.androidconventionplugins.dsl.androidLibrary
import com.example.androidconventionplugins.dsl.libs
import com.example.androidconventionplugins.dsl.plugin
import com.example.androidconventionplugins.dsl.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryModulePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.alias(libs.plugin("androidLibrary"))

            androidLibrary {
                compileSdk = libs.version("app-compileSdk").toInt()
                defaultConfig {
                    minSdk = libs.version("app-minSdk").toInt()
                }
            }

            dependencies {
                // Common dependencies
            }
        }
    }
}
