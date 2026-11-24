package io.github.hoge.redact.compat

import java.util.ServiceLoader
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

interface CompatContext {
    fun hasRedactOnNonDataClass(declaration: IrDeclaration): Boolean

    interface Factory {
        val minVersion: String
        fun create(): CompatContext
    }

    companion object {
        fun load(): CompatContext {
            val current = KotlinToolingVersion(KotlinCompilerVersion.VERSION)
            val factories = ServiceLoader.load(Factory::class.java, Factory::class.java.classLoader).toList()
            // pick the first factory that supports the current compiler
            val factory = factories.first { KotlinToolingVersion(it.minVersion) <= current }
            return factory.create()
        }
    }
}
