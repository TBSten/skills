package buildsrc.convention

// Maven Central publish convention (vanniktech maven-publish).
//
// - Coordinates: GROUP:<project.name>:VERSION_NAME, set once in the root build.gradle.kts.
//   Modules must NOT call `coordinates(...)`; they only set `pom { name; description }`.
// - Common POM fields (url / license / scm / developer) come from POM_* in gradle.properties.
// - `java-gradle-plugin` modules are detected automatically: the plugin marker
//   (`<plugin-id>.gradle.plugin`) is published to Maven Central too, so consumers can use
//   `plugins { id("<plugin-id>") version "x.y.z" }` with `mavenCentral()` only.
// - Release: `./gradlew publishAndReleaseToMavenCentral --no-configuration-cache`
//   (see .github/workflows/release.yml). Local check: `./gradlew publishToMavenLocal`.

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()

    // Sign only when credentials exist, so `publishToMavenLocal` works without a GPG key.
    // CI passes ORG_GRADLE_PROJECT_signingInMemoryKey (-> `signingInMemoryKey` property).
    val hasInMemoryKey = providers.gradleProperty("signingInMemoryKey").isPresent
    val hasKeyId = providers.gradleProperty("signing.keyId").isPresent
    if (hasInMemoryKey || hasKeyId) {
        signAllPublications()
    }
}
