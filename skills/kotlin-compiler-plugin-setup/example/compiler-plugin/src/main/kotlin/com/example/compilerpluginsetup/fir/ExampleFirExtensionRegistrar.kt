package com.example.compilerpluginsetup.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * FIR (frontend) extension registrar. FIR is the right place for validation and
 * early error reporting (accurate line numbers, IDE integration).
 *
 * This skeleton registers nothing so the freshly scaffolded project compiles.
 * FIR extensions are optional — an IR-only compiler plugin works fine.
 */
class ExampleFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        // Register FIR extensions (checkers, generators, ...) here, e.g.:
        //
        // +::ExampleFirCheckersExtension
        //
        // Keep FIR checkers best-effort (wrap in try-catch) and fall back to the
        // IR phase on failure.
    }
}
