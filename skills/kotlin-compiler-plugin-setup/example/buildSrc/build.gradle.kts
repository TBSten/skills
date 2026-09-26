plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinGradlePlugin)
    // `publish` convention plugin uses the `mavenPublishing { }` DSL of vanniktech maven-publish.
    implementation(libs.mavenPublishGradlePlugin)
}
