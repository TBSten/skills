// The build of the IDE plugin (IntelliJ IDEA / Android Studio, build 261 and up).
// Versions live in gradle/libs.versions.toml.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    // Same version as Kotlin.
    alias(libs.plugins.kotlin.compose.compiler)
    // Only for the standalone Compose Desktop of the headless preview.
    alias(libs.plugins.compose.multiplatform)
    // No version here: it is set in settings.gradle.kts, and setting it twice collides.
    id("org.jetbrains.intellij.platform")
}

group = "com.example.intellijplugindev"   // CUSTOMIZE
version = "0.1.0"              // CUSTOMIZE

// JBR 21, which the 261 platform runs on. If the default `java` is older, point JAVA_HOME
// (or -Dorg.gradle.java.home) at a JDK 21.
kotlin { jvmToolchain(21) }

// The UI Composables in src/shared/kotlin are compiled twice: into the plugin against the IDE's
// bundled Jewel, and into `preview` against standalone Jewel, so the headless PNGs show what ships.
sourceSets {
    main { kotlin.srcDir("src/shared/kotlin") }
    create("preview") { kotlin.srcDir("src/shared/kotlin") }
}
val previewImplementation: Configuration = configurations.getByName("previewImplementation")

dependencies {
    intellijPlatform {
        // 2026.1 = build 261. intellijIdeaCommunity(...) no longer resolves from 253 on.
        intellijIdea(libs.versions.intellijIdea.get())
        // Brings the Analysis API (K2), so analyze { } works without further dependencies.
        bundledPlugin("org.jetbrains.kotlin")
        // Jewel, Compose and Skiko come from the IDE rather than from the plugin. Each line pairs
        // with a <module name="..."/> in plugin.xml.
        bundledModule("intellij.platform.jewel.foundation")
        bundledModule("intellij.platform.jewel.ui")
        bundledModule("intellij.platform.jewel.ideLafBridge")
        bundledModule("intellij.libraries.compose.runtime.desktop")
        // Named explicitly because it does not put runtime on the compile classpath by itself.
        bundledModule("intellij.libraries.compose.foundation.desktop")
        bundledModule("intellij.libraries.skiko")
        // BasePlatformTestCase and friends.
        testFramework(TestFrameworkType.Platform)
    }
    // The platform no longer supplies JUnit 4, which BasePlatformTestCase is based on.
    testImplementation(libs.junit4)
    // The pure-JVM preview gates (PreviewChecks) are tested from `test`. Standalone Compose is
    // deliberately not put on the test classpath, where it would clash with the bundled one.
    // Side effect: the shared UI classes are on the test classpath twice (from main and from
    // preview). Harmless while tests do not load them.
    testImplementation(sourceSets["preview"].output)

    // renderComposeScene lives here, Skiko included.
    previewImplementation(compose.desktop.currentOs)
    val jewelForIde = libs.versions.jewelForIde.get()
    previewImplementation("org.jetbrains.jewel:jewel-int-ui-standalone:${libs.versions.jewel.get()}-$jewelForIde")
    // Without it AllIconsKeys render as magenta placeholders in the standalone preview.
    previewImplementation("com.jetbrains.intellij.platform:icons:$jewelForIde")
}

intellijPlatform {
    // A small plugin: skip starting a headless IDE to index settings.
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
            // No upper bound: every future build is declared compatible. That promise is only
            // safe with `./gradlew verifyPlugin` (configured below) running on CI; otherwise
            // narrow it to "261.*".
            untilBuild = provider { null }
        }
    }
    // `./gradlew verifyPlugin` checks binary compatibility against the IDEs recommended for the
    // since/until range above. It downloads those IDEs, so run it on CI rather than every build.
    pluginVerification {
        ides {
            recommended()
        }
    }
}

// K2 for the Analysis API in tests, paired with <supportsKotlinPluginMode supportsK2="true"/>.
// No useJUnitPlatform(): BasePlatformTestCase is JUnit 4 based.
tasks.test { systemProperty("idea.kotlin.plugin.use.k2", "true") }

// updatePreview / verifyPreview: run the preview main() on the standalone classpath.
// The first argument picks the mode; the working directory is this project directory.
fun registerPreviewTask(name: String, mode: String, desc: String) = tasks.register<JavaExec>(name) {
    group = "preview"
    description = desc
    mainClass.set("com.example.intellijplugindev.preview.PreviewMainKt")
    classpath = sourceSets["preview"].runtimeClasspath
    jvmArgs("-Djava.awt.headless=true", "-Dskiko.renderApi=SOFTWARE")
    args(mode)
}
registerPreviewTask("updatePreview", "update", "Render preview PNGs, write the gallery, and force-refresh the golden snapshots.")
registerPreviewTask("verifyPreview", "verify", "Render preview PNGs and fail the build if any differs from the golden snapshots (VRT gate).")
