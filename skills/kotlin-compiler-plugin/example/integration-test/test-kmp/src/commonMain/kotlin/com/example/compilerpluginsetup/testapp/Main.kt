package com.example.compilerpluginsetup.testapp

/**
 * KMP end-to-end test shared by all targets (JVM + JS). The compiler plugin is applied
 * to every target via `kotlinCompilerPluginClasspath(project(":compiler-plugin"))`.
 *
 * Run with `./gradlew :integration-test:test-kmp:jvmRun` (JVM target).
 */
fun main() {
    // TODO: Call your runtime API here and verify the value the compiler plugin produced
    //       on each target.
    val result = "not-yet-transformed"
    check(result.isNotEmpty()) { "Unexpected result: $result" }
    println("OK: $result")
}
