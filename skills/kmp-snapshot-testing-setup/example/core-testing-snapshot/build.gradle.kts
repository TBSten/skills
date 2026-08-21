// core/testing/snapshot モジュールの build.gradle.kts テンプレート。
// kmp-snapshot-testing-setup の scripts/install.sh がモジュールルートに配置する
// (このファイルは Kotlin ソースと違いパッケージ置換の対象ではない)。
//
// カスタマイズポイント (インストール後に AI がプロジェクトに合わせて調整する):
// - プロジェクトに KMP 共通 convention plugin (例: convention-kmp) がある場合は
//   plugins ブロックと kotlin { jvm() } をそちらに寄せてよい。
// - plugins は root プロジェクト等で version 宣言済みであることを前提に id() で適用している。
//   alias(libs.plugins.*) 方式のプロジェクトでは合わせて書き換える。
// - `org.jetbrains.kotlin.plugin.serialization` が build classpath に無い場合は
//   root build.gradle.kts 等で version 付きで宣言する (SKILL.md の AI フォローアップ参照)。
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    // compose-deps:start (install.sh --skip-compose 時にこのブロックは削除される)
    id("org.jetbrains.compose")
    // compose-deps:end
}

kotlin {
    jvm()

    sourceSets {
        named("jvmMain") {
            dependencies {
                // スナップショットテスト基盤の公開 API (Spec 基底クラス・Arb・assertion) が
                // これらの型を露出するため api で宣言する。
                api(kotlin("test"))
                api(libs.kotestFrameworkEngine)
                api(libs.kotestAssertionsCore)
                api(libs.kotestProperty)
                // JUnit Platform 経由の実行 (jvmSnapshotTest*) とレポーターに必要
                api(libs.kotestRunnerJunit5)
                api(libs.kotestExtensionsHtmlReporter)
                api(libs.kotestExtensionsJunitXml)
                api(libs.kotlinx.coroutines.test)
                // KotlinCodeFormat (kotlinx.serialization の Kotlin Code 出力) に必要
                api(libs.kotlinxSerializationCore)
                // compose-deps:start (install.sh --skip-compose 時にこのブロックは削除される)
                // ImageSnapshotAssertion / SemanticsSnapshotAssertion (Compose 向け assertion) に必要
                api(compose.ui)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.uiTest)
                // compose-deps:end
            }
        }
    }
}
