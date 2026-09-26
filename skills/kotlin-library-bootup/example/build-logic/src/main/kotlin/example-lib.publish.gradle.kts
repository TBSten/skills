import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.KotlinMultiplatform

// Maven Central へ出すモジュールの共通設定。publish しないモジュールには適用しない
// (適用しなければ publish タスクも生えない)。
plugins {
    id("com.vanniktech.maven.publish")
    // HTML 出力 (root の generateApiDocs で集約) と javadoc jar の中身に使う
    id("org.jetbrains.dokka")
}

dokka {
    moduleName.set(project.name)
    dokkaSourceSets.configureEach {
        sourceLink {
            // localDirectory は既定でモジュールのルートなので、ファイルの相対パスと行番号 (#L) が自動で付く
            remoteUrl("https://github.com/<owner>/<repo>/blob/main/${project.path.removePrefix(":").replace(":", "/")}")
        }
    }
}

// Central は sources jar と javadoc jar の両方を必須にしている。Kotlin には javadoc が無いので
// Dokka の出力を javadoc jar として包む。タスク名は "dokkaGenerate" (中身を持たない lifecycle タスク)
// ではなく、@OutputDirectory を持つ実体の dokkaGeneratePublication* を渡すこと (空 jar になる)。
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    mavenPublishing {
        // KMP は Dokka javadoc 形式が JVM 以外を扱えないため HTML を包む
        configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"), sourcesJar = true))
    }
}
pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    // Javadoc 形式は Dokka 2.x では別 plugin
    pluginManager.apply("org.jetbrains.dokka-javadoc")
    mavenPublishing {
        configure(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc"), sourcesJar = true))
    }
}

mavenPublishing {
    publishToMavenCentral()

    // artifactId は project path から導出する (:example-lib-core -> example-lib-core, :a:b -> a-b)。
    // integrationTest の composite build は group + project.name で置換するので、トップレベルに置く限り一致する。
    coordinates(group.toString(), project.path.removePrefix(":").replace(":", "-"), version.toString())

    pom {
        name.set(project.name)
        description.set("<description>")
        inceptionYear.set("<year>")
        url.set("https://github.com/<owner>/<repo>")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/<owner>/<repo>/blob/main/LICENSE")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("<developer-id>")
                name.set("<developer-name>")
                url.set("https://github.com/<developer-id>")
            }
        }

        scm {
            url.set("https://github.com/<owner>/<repo>")
            connection.set("scm:git:git://github.com/<owner>/<repo>.git")
            developerConnection.set("scm:git:ssh://git@github.com/<owner>/<repo>.git")
        }
    }

    // ローカル publish (publishToMavenLocal) の時だけ署名をスキップする。
    // CI の Maven Central publish は ORG_GRADLE_PROJECT_signingInMemoryKey* を注入するので常に署名する。
    // `:example-lib-core:publishToMavenLocal` のような path 付き指定でも効くよう末尾セグメントで判定する。
    // 明示的に止めたい時は -PskipSigning=true。
    val isLocalPublish = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "publishToMavenLocal" }
    val skipSigning = providers.gradleProperty("skipSigning").orNull?.toBoolean() == true
    if (!isLocalPublish && !skipSigning) signAllPublications()
}
