// NOTE: buildSrc/src/main/kotlin/ 直下に配置する。package 宣言を付けると
// precompiled script plugin の ID が「パッケージ名.publish-convention」に変わり
// id("publish-convention") で適用できなくなるため、package 宣言は付けないこと。
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Sign only when GPG key is available (CI or local with signing configured).
    if (project.findProperty("signing.keyId") != null ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null
    ) {
        signAllPublications()
    }

    pom {
        name.set(project.name)
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
