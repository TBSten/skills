package com.example.kotlinlibrarybootup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ExampleLibSpec :
    FreeSpec({
        "greet" - {
            "名前を渡すと既定の前置きで挨拶文を返す" {
                ExampleLib().greet("Kotlin") shouldBe "Hello, Kotlin!"
            }

            "前置きを変えると挨拶文の前置きも変わる" {
                ExampleLib(greeting = "Hi").greet("Kotlin") shouldBe "Hi, Kotlin!"
            }

            "空白だけの名前は理由付きで拒否する" {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        ExampleLib().greet("  ")
                    }
                error.message shouldContain "blank"
            }
        }

        "greetAll は渡した順に全員へ挨拶する" {
            ExampleLib().greetAll(listOf("A", "B")) shouldContainExactly listOf("Hello, A!", "Hello, B!")
        }
    })
