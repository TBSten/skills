package com.example.compilerpluginsetup.testapp

/**
 * JVM end-to-end test. The compiler plugin is applied via
 * `kotlinCompilerPluginClasspath(project(":compiler-plugin"))`.
 *
 * `check()` exits with a non-zero code on failure, so `./gradlew :integration-test:test-jvm:run`
 * fails in CI when the transformation regresses.
 */
fun main() {
    // TODO: Call your runtime API here and verify the value the compiler plugin produced.
    val result = "not-yet-transformed"
    check(result.isNotEmpty()) { "Unexpected result: $result" }
    println("OK: $result")
}
