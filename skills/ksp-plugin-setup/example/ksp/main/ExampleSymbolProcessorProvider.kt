package com.example.ksppluginsetup.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP entry point. Registered via
 * `src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
 * containing this class's fully-qualified name.
 */
public class ExampleSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ExampleSymbolProcessor(
            rawOptions = environment.options,
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
}
