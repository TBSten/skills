package com.example.kotlinlibrarybootup.integration

import com.example.kotlinlibrarybootup.ExampleLib
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

/**
 * 利用者と同じ経路 (Maven 座標 + composite build) でライブラリを使えることを確かめる。
 * opt-in 必須の API (@ExperimentalExampleLibApi 等) の壁も、ここでしか確かめられない。
 */
class ConsumerSpec :
    FreeSpec({
        "公開 API を外部ビルドから呼び出せる" {
            ExampleLib().greet("integration") shouldBe "Hello, integration!"
        }
    })
