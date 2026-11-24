package com.example.compilerpluginsetup.gradle

import org.gradle.api.provider.Property

/**
 * `examplePlugin { ... }` DSL. Defaults are set with `convention(...)` in
 * [ExampleGradlePlugin.apply], so users only write what they want to change.
 *
 * Each property is passed to the compiler plugin as a `SubpluginOption` and must have a
 * matching `CliOption` in `ExampleCommandLineProcessor`.
 */
abstract class ExampleExtension {
    /** `false` makes the compiler plugin register no FIR / IR extension at all. */
    abstract val enabled: Property<Boolean>

    /** Automatically add the runtime artifact to the consumer's dependencies. */
    abstract val addRuntimeDependency: Property<Boolean>

    companion object {
        const val NAME = "examplePlugin"
    }
}
