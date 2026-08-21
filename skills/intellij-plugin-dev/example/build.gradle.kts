// IntelliJ Platform plugin の build 配線の完成形 (コード片の SSoT)。
// 設計解説: references/setup/basics.md (基本) / setup/preview.md (preview) / setup/snapshot.md (VRT golden)。
// バージョンの SSoT は gradle/libs.versions.toml。
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    // Compose Compiler (Kotlin と同版に揃える — setup/basics.md)
    alias(libs.plugins.kotlin.compose.compiler)
    // preview の standalone Compose Desktop 用 (setup/preview.md)
    alias(libs.plugins.compose.multiplatform)
    // version は settings.gradle.kts 側にのみ書く (両方に書くと classpath 衝突 — setup/basics.md)
    id("org.jetbrains.intellij.platform")
}

group = "com.example.plugin"   // CUSTOMIZE
version = "0.1.0"              // CUSTOMIZE

// JBR 21 (setup/basics.md)。マシン既定 java が 17 なら JAVA_HOME / -Dorg.gradle.java.home で明示する
kotlin { jvmToolchain(21) }

// --- source set 共有 (setup/preview.md):
// 図/UI の Composable (src/shared/kotlin) を plugin 本体 (bundled Jewel) と preview (standalone Jewel)
// の両方の srcDir に足し、それぞれの Compose 依存で二重コンパイルする。
sourceSets {
    main { kotlin.srcDir("src/shared/kotlin") }
    create("preview") { kotlin.srcDir("src/shared/kotlin") }
}
val previewImplementation: Configuration by configurations.getting

dependencies {
    intellijPlatform {
        // build 261 = 2026.1 (AS Quail 2026.1.1 と同世代)。intellijIdeaCommunity は 253 以降解決不可
        intellijIdea(libs.versions.intellijIdea.get())
        // Analysis API (K2) 同梱 = 追加依存なしで analyze{}/KaSession が載る
        bundledPlugin("org.jetbrains.kotlin")
        // Jewel/Compose/Skiko は 261 バンドルを引く (自前 Compose を持たない)。
        // plugin.xml の <dependencies><module name="..."/> と対を成す (setup/basics.md)
        bundledModule("intellij.platform.jewel.foundation")
        bundledModule("intellij.platform.jewel.ui")
        bundledModule("intellij.platform.jewel.ideLafBridge")
        bundledModule("intellij.libraries.compose.runtime.desktop")
        bundledModule("intellij.libraries.compose.foundation.desktop")  // compile classpath に runtime を伝播しないので明示
        bundledModule("intellij.libraries.skiko")
        // BasePlatformTestCase など (詳細は analysis-api-testing.md)
        testFramework(TestFrameworkType.Platform)
    }
    // 2.0.0-rc1 以降 Platform は JUnit4 を供給しない。BasePlatformTestCase は JUnit4 系
    testImplementation(libs.junit4)
    // preview の純出力ゲート (PreviewChecks) を test から叩く。standalone Compose 依存は載せない
    // (bundled Compose との二重ロードを避ける — setup/snapshot.md)
    testImplementation(sourceSets["preview"].output)

    // --- preview (standalone) 側の依存 (setup/preview.md)
    // renderComposeScene はここ (Skiko 同梱)。uiTestJUnit4 は不要
    previewImplementation(compose.desktop.currentOs)
    val jewelForIde = libs.versions.jewelForIde.get()
    previewImplementation("org.jetbrains.jewel:jewel-int-ui-standalone:${libs.versions.jewel.get()}-$jewelForIde")
    // AllIconsKeys を standalone preview でも解決させる (無いとマゼンタのプレースホルダになる — headless-preview.md)
    previewImplementation("com.jetbrains.intellij.platform:icons:$jewelForIde")
}

intellijPlatform {
    // 小さい plugin は省く (headless IDE 起動を避ける — setup/basics.md)
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"             // floor = build 261 (AS Quail 2026.1.1 / IJ 2026.1)
            // 上限無し = 全ての将来 build に互換と宣言。verifyPlugin (Plugin Verifier) の CI ゲートと
            // セットで運用する。互換を 261 系に絞るなら untilBuild = "261.*" (setup/basics.md)
            untilBuild = provider { null }
        }
    }
}

// AA を K2 で動かす (plugin.xml の <supportsKotlinPluginMode supportsK2="true"/> とセット)。
// test に useJUnitPlatform() を付けない (BasePlatformTestCase は JUnit4 — setup/basics.md)
tasks.test { systemProperty("idea.kotlin.plugin.use.k2", "true") }

// --- preview タスク (setup/preview.md): preview の main() を standalone 依存で回す。
// 第 1 引数で mode を切る (update / verify)。update / verify の golden 側の意味は setup/snapshot.md。
fun registerPreviewTask(name: String, mode: String, desc: String) = tasks.register<JavaExec>(name) {
    group = "preview"
    description = desc
    mainClass.set("com.example.plugin.preview.PreviewMainKt")
    classpath = sourceSets["preview"].runtimeClasspath
    jvmArgs("-Djava.awt.headless=true", "-Dskiko.renderApi=SOFTWARE")
    args(mode)
}
registerPreviewTask("updatePreview", "update", "Render preview PNGs, write the gallery, and force-refresh the golden snapshots.")
registerPreviewTask("verifyPreview", "verify", "Render preview PNGs and fail the build if any differs from the golden snapshots (VRT gate).")
