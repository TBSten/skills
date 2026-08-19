package com.example.ksppluginsetup.ksp.feature.greeting.scenario

import com.example.ksppluginsetup.Greeting
import com.example.ksppluginsetup.ksp.testing.generator.Generator
import com.example.ksppluginsetup.ksp.testing.poet.Prop
import com.example.ksppluginsetup.ksp.testing.poet.SnapshotScenario
import com.example.ksppluginsetup.ksp.testing.poet.dataClass
import com.example.ksppluginsetup.ksp.testing.poet.snapshotScenarios
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING

private val GREETING = AnnotationSpec.builder(Greeting::class).build()

/**
 * One `scenario/` file per **family** (a single axis of variation). Families are unioned in the
 * snapshot spec and then crossed with the option axis — so adding a case here costs one golden file
 * per representative option set, not a new test method.
 */
internal fun propertyShapeScenarios(): Generator<SnapshotScenario> =
    Generator.snapshotScenarios(
        "singleProperty" to
            SnapshotScenario(dataClass("User", Prop("name"), annotations = listOf(GREETING))),
        "multipleProperties" to
            SnapshotScenario(
                dataClass("User", Prop("name"), Prop("age", INT), annotations = listOf(GREETING)),
            ),
        "nullableProperty" to
            SnapshotScenario(
                dataClass("User", Prop("nickname", STRING.copy(nullable = true)), annotations = listOf(GREETING)),
            ),
        "noProperty" to
            SnapshotScenario(dataClass("Marker", annotations = listOf(GREETING))),
    )
