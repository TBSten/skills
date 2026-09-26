// JVM only モジュールの共通構成 (explicitApi / toolchain / 下位互換の床)。
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("example-lib.lint")
    id("example-lib.test")
}

kotlin {
    explicitApi()
    jvmToolchain(libs.version("jvmToolchain").toInt())

    compilerOptions {
        configureCommon(project)
    }
    configureCompatibilityFloor(project)
}

// KMP 構成と同じコマンド (./gradlew jvmTest / allTests) で回せるようにするエイリアス。
// README / CLAUDE.md / CI の文言をターゲット構成に依らず揃えるため。
tasks.register("jvmTest") {
    group = "verification"
    description = "Alias of test (for parity with Kotlin Multiplatform modules)."
    dependsOn(tasks.named("test"))
}
tasks.register("allTests") {
    group = "verification"
    description = "Alias of test (for parity with Kotlin Multiplatform modules)."
    dependsOn(tasks.named("test"))
}
