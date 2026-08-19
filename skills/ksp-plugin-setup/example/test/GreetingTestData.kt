// test/src/commonMain/kotlin/com/example/ksppluginsetup/test/greeting/GreetingTestData.kt
//
// The `test` module's commonMain holds the ANNOTATED INPUT; commonTest holds the verification, one
// file per input file. KSP runs for real here, so this is where "does the generated code actually
// behave correctly on every target" is answered — a question the JVM-only kctfork suite cannot ask.
package com.example.ksppluginsetup.test.greeting

import com.example.ksppluginsetup.Greeting

@Greeting
data class User(
    val name: String,
    val age: Int,
    @Greeting.Exclude val passwordHash: String,
)
