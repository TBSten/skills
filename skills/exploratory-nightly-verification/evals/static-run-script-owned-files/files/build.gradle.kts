plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "dev.example"
version = "0.3.0"

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.kotest.runner)
}
