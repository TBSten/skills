# Publish Convention Plugin

Maven Central に公開するための convention plugin パターン。

## buildSrc の設定

`buildSrc/build.gradle.kts` に Vanniktech Maven Publish plugin の依存を追加:

```kotlin
dependencies {
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.plugins.mavenPublish.map {
        "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
    })
}
```

`gradle/libs.versions.toml` に追加:

```toml
[versions]
mavenPublish = "0.30.0"

[plugins]
mavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "mavenPublish" }
```

## Convention Plugin

`buildSrc/src/main/kotlin/publish-convention.gradle.kts`:

```kotlin
package buildsrc.convention

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    if (project.findProperty("signing.keyId") != null ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null
    ) {
        signAllPublications()
    }

    pom {
        name.set(project.name)
        description.set("<your-project-description>")
        url.set("https://github.com/<your-repo>")
        inceptionYear.set("<year>")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("<your-id>")
                name.set("<your-name>")
                url.set("https://github.com/<your-id>")
            }
        }

        scm {
            url.set("https://github.com/<your-repo>")
            connection.set("scm:git:git://github.com/<your-repo>.git")
            developerConnection.set("scm:git:ssh://git@github.com/<your-repo>.git")
        }
    }
}
```

## 各モジュールでの使用

```kotlin
plugins {
    id("buildsrc.convention.publish-convention")
}

mavenPublishing {
    coordinates(
        groupId = "<your-group-id>",
        artifactId = "<artifact-id>",
        version = libs.versions.<your-version>.get(),
    )
}
```
