// NOTE: buildSrc (または build-logic) の src/main/kotlin/ 直下に配置する。package 宣言を付けると
// precompiled script plugin の ID が「パッケージ名.publish-convention」に変わり
// id("publish-convention") で適用できなくなるため、package 宣言は付けないこと。
//
// 各モジュールは id("publish-convention") を適用し、必要なら
//   mavenPublishing { pom { description.set("...") } }
// で pom の name / description だけ上書きする。coordinates(...) はモジュール側で呼ばない
// (座標の SSoT はこの convention。二重設定は "... is final" エラーの原因になる)。
plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    // Central Portal (https://central.sonatype.com) へ publish する。
    // vanniktech 0.34.0 で SonatypeHost は DSL から削除済み (OSSRH 終了のため)。引数なしで Central Portal になる。
    // gradle.properties に mavenCentralPublishing=true を書く場合はこの呼び出しと二重になるので、どちらか一方にする。
    publishToMavenCentral()

    // artifactId は project.path から導出する (":lib:core" -> "lib-core")。
    // ネストしたモジュールでも Maven 座標がフラットで衝突しない。トップレベルモジュールでは project.name と同じ。
    val flatArtifactId = project.path.removePrefix(":").replace(":", "-").ifEmpty { project.name }
    coordinates(artifactId = flatArtifactId)

    // publishToMavenLocal (および publishXxxPublicationToMavenLocal) の時だけ署名をスキップする。
    // GPG 鍵の無いローカルでも動作確認でき、CI の本番 publish では鍵が無ければ fail fast する。
    // ":lib:publishToMavenLocal" のように path 付きで呼ばれても判定できるよう末尾セグメントで見る。
    // (鍵の有無で切り替える方式との比較は references/convention-options.md)
    val isLocalPublish = gradle.startParameter.taskNames.any { it.substringAfterLast(':').endsWith("ToMavenLocal") }
    if (!isLocalPublish) {
        signAllPublications()
    }

    pom {
        name.set(flatArtifactId)
        description.set("<PROJECT_DESCRIPTION>")
        url.set("<GITHUB_URL>")
        inceptionYear.set("<INCEPTION_YEAR>")

        licenses {
            license {
                name.set("<LICENSE_NAME>")
                url.set("<LICENSE_URL>")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("<DEVELOPER_ID>")
                name.set("<DEVELOPER_NAME>")
                url.set("<DEVELOPER_URL>")
            }
        }

        scm {
            url.set("<GITHUB_URL>")
            connection.set("scm:git:git://github.com/<GITHUB_OWNER>/<GITHUB_REPO>.git")
            developerConnection.set("scm:git:ssh://git@github.com/<GITHUB_OWNER>/<GITHUB_REPO>.git")
        }
    }
}
