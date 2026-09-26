// ライブラリの利用者を模した JVM モジュール。publish される形 (Maven 座標) で依存する。
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

dependencies {
    testImplementation(libs.example.lib.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
}
