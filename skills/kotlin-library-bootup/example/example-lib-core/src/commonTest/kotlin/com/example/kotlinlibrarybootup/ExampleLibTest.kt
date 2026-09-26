package com.example.kotlinlibrarybootup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExampleLibTest {
    @Test
    fun 名前を渡すと既定の前置きで挨拶文を返す() {
        ExampleLib().greet("Kotlin") shouldBe "Hello, Kotlin!"
    }

    @Test
    fun 前置きを変えると挨拶文の前置きも変わる() {
        ExampleLib(greeting = "Hi").greet("Kotlin") shouldBe "Hi, Kotlin!"
    }

    @Test
    fun 空白だけの名前は拒否する() {
        shouldThrow<IllegalArgumentException> {
            ExampleLib().greet("  ")
        }
    }

    @Test
    fun 複数人へまとめて挨拶できる() {
        ExampleLib().greetAll(listOf("A", "B")) shouldContainExactly listOf("Hello, A!", "Hello, B!")
    }
}
