package io.github.hoge.redact.compat.k220

import io.github.hoge.redact.compat.CompatContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

class CompatContextImpl : CompatContext {
    override fun hasRedactOnNonDataClass(declaration: IrDeclaration): Boolean = TODO("2.2 impl")

    class Factory : CompatContext.Factory {
        override val minVersion: String = "2.2.0"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
