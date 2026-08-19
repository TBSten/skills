// test/src/commonTest/kotlin/com/example/ksppluginsetup/test/greeting/GreetingTest.kt
package com.example.ksppluginsetup.test.greeting

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * kotest `FreeSpec` everywhere, with `actual shouldBe expected` word order (the opposite of
 * `kotlin.test`'s `assertEquals(expected, actual)`), and `withClue` when a failure needs context.
 */
class GreetingTest :
    FreeSpec({
        "the generated greeting includes every non-excluded property" {
            User(name = "Ada", age = 36, passwordHash = "***").greetUser() shouldBe "Hi, User(name, age)"
        }

        "an @Greeting.Exclude property is left out" {
            User(name = "Ada", age = 36, passwordHash = "***").greetUser() shouldNotContain "passwordHash"
        }
    })
