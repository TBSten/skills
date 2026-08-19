package com.example.ksppluginsetup.ksp.feature.greeting.scenario

import com.example.ksppluginsetup.Greeting
import com.example.ksppluginsetup.ksp.testing.generator.Generator
import com.example.ksppluginsetup.ksp.testing.poet.Prop
import com.example.ksppluginsetup.ksp.testing.poet.SnapshotScenario
import com.example.ksppluginsetup.ksp.testing.poet.dataClass
import com.example.ksppluginsetup.ksp.testing.poet.snapshotScenarios
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.INT

private val GREETING = AnnotationSpec.builder(Greeting::class).build()
private val EXCLUDE = AnnotationSpec.builder(Greeting.Exclude::class).build()

internal fun excludeScenarios(): Generator<SnapshotScenario> =
    Generator.snapshotScenarios(
        "excludedProperty" to
            SnapshotScenario(
                dataClass(
                    "User",
                    Prop("name"),
                    Prop("secret", INT, paramAnnotation = EXCLUDE),
                    annotations = listOf(GREETING),
                ),
            ),
        "allPropertiesExcluded" to
            SnapshotScenario(
                dataClass(
                    "User",
                    Prop("name", paramAnnotation = EXCLUDE),
                    Prop("age", INT, paramAnnotation = EXCLUDE),
                    annotations = listOf(GREETING),
                ),
            ),
    )
