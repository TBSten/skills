package com.example.compilerpluginsetup.runtime

/**
 * Placeholder of the runtime API that the compiler plugin looks for.
 *
 * TODO: Replace with your real API (annotations / functions whose calls the IR transformer rewrites).
 * The runtime needs at least one declaration: a KMP module without sources produces no klib and
 * `publishToMavenLocal` / Maven Central publishing fails.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExampleMarker
