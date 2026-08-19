package com.example.ksppluginsetup.ksp.testing.poet

import com.example.ksppluginsetup.ksp.testing.generator.Generator
import com.example.ksppluginsetup.ksp.testing.generator.generator
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of

/**
 * Snapshot inputs are **built with KotlinPoet**, not written as string literals.
 *
 * That is what makes a scenario family composable: a "property shape" axis and an "exclude" axis can
 * be crossed mechanically, and every case renders through one formatter so goldens never churn on
 * whitespace. (Generation itself still uses plain string append — KotlinPoet is a test-only tool.)
 */
internal data class SnapshotScenario(
    val files: List<FileSpec>,
) {
    constructor(vararg types: TypeSpec, packageName: String = "test") :
        this(types.map { FileSpec.builder(packageName, it.name!!).addType(it).build() })
}

/** One property of a generated input class. */
internal data class Prop(
    val name: String,
    val type: TypeName = STRING,
    val paramAnnotation: AnnotationSpec? = null,
)

/** A `data class` input with the given properties. */
internal fun dataClass(
    name: String,
    vararg props: Prop,
    annotations: List<AnnotationSpec> = emptyList(),
): TypeSpec =
    TypeSpec
        .classBuilder(name)
        .addModifiers(KModifier.DATA)
        .apply { annotations.forEach { addAnnotation(it) } }
        .primaryConstructor(
            FunSpec
                .constructorBuilder()
                .apply {
                    props.forEach { prop ->
                        addParameter(
                            ParameterSpec
                                .builder(prop.name, prop.type)
                                .apply { prop.paramAnnotation?.let { addAnnotation(it) } }
                                .build(),
                        )
                    }
                }.build(),
        ).apply {
            props.forEach { prop ->
                addProperty(PropertySpec.builder(prop.name, prop.type).initializer(prop.name).build())
            }
        }.build()

/** Turn labelled scenarios into a [Generator] so they can be crossed with the option axis. */
internal fun Generator.Companion.snapshotScenarios(vararg scenarios: Pair<String, SnapshotScenario>): Generator<SnapshotScenario> =
    generator {
        scenarios.forEach { (label, scenario) -> label case scenario }
        Arb.of(scenarios.map { it.second })
    }
