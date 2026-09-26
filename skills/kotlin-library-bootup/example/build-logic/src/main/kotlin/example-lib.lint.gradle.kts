// ktlint (ルールは .editorconfig が SSoT)。./gradlew ktlintCheck / ktlintFormat
plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set(libs.version("ktlint"))
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
