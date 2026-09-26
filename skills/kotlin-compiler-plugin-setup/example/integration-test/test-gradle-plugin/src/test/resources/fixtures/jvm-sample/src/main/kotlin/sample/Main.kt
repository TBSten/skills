package sample

import com.example.compilerpluginsetup.runtime.ExampleMarker

// Compiles only if the Gradle plugin added the runtime dependency automatically.
// TODO: Call your runtime API here, print the value the compiler plugin produced and assert on
//  it in ExampleGradlePluginE2eTest.
@ExampleMarker
fun marked() = "ok"

fun main() {
    println("E2E_RESULT=${marked()}")
}
