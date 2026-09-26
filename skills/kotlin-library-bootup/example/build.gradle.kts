// @bootup:if kmp
import kotlinx.validation.ExperimentalBCVApi

// @bootup:end
plugins {
    // 各モジュールは build-logic の convention plugin (example-lib.*) 経由で適用する。
    // ここで apply(false) しておくと全モジュールが同じ classloader の plugin を使う。
    // @bootup:if kmp
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    // @bootup:end
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.maven.publish).apply(false)
    alias(libs.plugins.ktlint).apply(false)
    // Dokka はルートで全モジュールの HTML を集約する (generateApiDocs)
    alias(libs.plugins.dokka)
    // 公開 API の互換性チェック (apiDump / apiCheck)
    alias(libs.plugins.binary.compatibility.validator)
}

allprojects {
    group = "<group-id>"
    // バージョンの SSoT は gradle/libs.versions.toml の example-lib
    version = rootProject.libs.versions.example.lib.get()
}

apiValidation {
    // @bootup:if kmp
    // JVM 以外 (JS / Wasm / Native) の ABI も klib dump で追跡する
    @OptIn(ExperimentalBCVApi::class)
    klib.enabled = true
    // @bootup:end
    // opt-in 必須の API は互換性保証の対象外にする
    nonPublicMarkers.addAll(
        listOf(
            "com.example.kotlinlibrarybootup.InternalExampleLibApi",
            "com.example.kotlinlibrarybootup.ExperimentalExampleLibApi",
        ),
    )
}

// CI / release 作業からライブラリのバージョンを取得する: ./gradlew -q logVersion
val logVersion by tasks.registering {
    group = "help"
    description = "Prints the library version defined in gradle/libs.versions.toml."
    val version = libs.versions.example.lib
    doLast {
        println(version.get())
    }
}

// 全モジュールの Dokka HTML を 1 サイトに集約する
dokka {
    moduleName.set(rootProject.name)
    dokkaPublications.named("html") {
        // @bootup:if docs-site
        // docs サイト (Astro Starlight) の public/ に直接出力し、/api-docs/ として配信する
        outputDirectory.set(layout.projectDirectory.dir("docs/public/api-docs"))
        // @bootup:end
        // @bootup:if !docs-site
        //~ outputDirectory.set(layout.buildDirectory.dir("api-docs"))
        // @bootup:end
    }
}

dependencies {
    dokka(project(":example-lib-core"))
}

// CI (.github/workflows/docs.yml) はこのタスク名だけを叩く。Dokka のタスク名が変わっても
// ここを直せば済むようにするためのラッパー。
tasks.register("generateApiDocs") {
    group = "documentation"
    description = "Aggregates the Dokka HTML of all published modules for the docs site."
    dependsOn("dokkaGenerateHtml")
}
