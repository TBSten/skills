// ui/core/testing モジュール (Compose PBT テスト基盤) の build.gradle.kts テンプレート。
// kmp-snapshot-testing-setup の scripts/install.sh がモジュールルートに配置し、
// `:core:testing:snapshot` を --module-path から計算した Gradle パスに置換する。
//
// カスタマイズポイント (インストール後に AI がプロジェクトに合わせて調整する):
// - プロジェクトに KMP 共通 convention plugin (例: convention-kmp) がある場合は
//   plugins ブロックと kotlin { jvm() } をそちらに寄せてよい。
// - plugins は root プロジェクト等で version 宣言済みであることを前提に id() で適用している。
//   alias(libs.plugins.*) 方式のプロジェクトでは合わせて書き換える。
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()

    sourceSets {
        named("jvmMain") {
            dependencies {
                // スナップショットテスト基盤モジュール (install.sh が置換する)
                api(project(":core:testing:snapshot"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.ui)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.uiTest)
                // JVM デスクトップでのスクリーンショット描画 (runComposeUiTest) に必要
                implementation(compose.desktop.currentOs)
                // TODO: AppTheme / WithTestGraph を提供するモジュールへの依存を追加する
                //   例: implementation(project(":ui:core"))
                //   (ComposeSnapshot.kt の AppTheme.Provider / WithTestGraph 参照を
                //    プロジェクトの theme / DI 構成に合わせて調整すること)
            }
        }
    }
}
