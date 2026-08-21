package com.example.compilerpluginsetup

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * IR transformer. This skeleton is a no-op so the freshly scaffolded project compiles;
 * override `visitCall` / `visitFunction` etc. to implement your transformation.
 */
class ExampleTransformer(
    @Suppress("unused") private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {
    // TODO: Override visit methods to implement the actual IR transformation, e.g.:
    //
    // override fun visitCall(expression: IrCall): IrExpression {
    //     // rewrite calls to your runtime API here
    //     return super.visitCall(expression)
    // }
}
