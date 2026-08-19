package com.example.ksppluginsetup.ksp

import com.example.ksppluginsetup.ksp.options.ExampleOptions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver

/**
 * Per-round infrastructure shared by every feature entry point.
 *
 * [logger] is deliberately **non-null**: the KSP environment always provides one, and a nullable
 * logger forces every generator down the chain to carry a fallback branch that can only throw.
 *
 * This is **leaf** infrastructure — it must not import `feature` or `core`. That keeps
 * `feature → ProcessContext` (the single upward dependency in the project) acyclic. The generation
 * layer never takes the whole context; it declares the narrower capabilities it actually needs, e.g.
 * `context(options: ExampleOptions, logger: KSPLogger)`.
 */
internal class ProcessContext(
    val resolver: Resolver,
    val options: ExampleOptions,
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
)
