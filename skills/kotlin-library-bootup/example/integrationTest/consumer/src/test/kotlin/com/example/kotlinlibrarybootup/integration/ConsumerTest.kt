package com.example.kotlinlibrarybootup.integration

import com.example.kotlinlibrarybootup.ExampleLib
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * 利用者と同じ経路 (Maven 座標 + composite build) でライブラリを使えることを確かめる。
 */
class ConsumerTest {
    @Test
    fun 公開APIを外部ビルドから呼び出せる() {
        ExampleLib().greet("integration") shouldBe "Hello, integration!"
    }
}
