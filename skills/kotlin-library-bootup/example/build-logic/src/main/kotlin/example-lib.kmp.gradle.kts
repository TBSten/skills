// @bootup:file-if kmp
// Kotlin Multiplatform ライブラリモジュールの共通構成 (ターゲット一式 / explicitApi / 下位互換の床)。
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("example-lib.lint")
    id("example-lib.test")
}

kotlin {
    configureExampleLibTargets(project)
}
