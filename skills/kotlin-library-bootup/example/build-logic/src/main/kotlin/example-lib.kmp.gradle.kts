// @bootup:file-if kmp
// Kotlin Multiplatform ライブラリモジュールの共通構成 (ターゲット一式 / explicitApi / 下位互換の床)。
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    // kotest を JS / Wasm / Native でも動かすための KSP + kotest plugin。
    // kotest plugin は KSP ベースで spec の launcher を生成するので、必ず KSP の後に適用する。
    id("com.google.devtools.ksp")
    id("io.kotest")
    id("example-lib.lint")
    id("example-lib.test")
}

kotlin {
    configureExampleLibTargets(project)
}
