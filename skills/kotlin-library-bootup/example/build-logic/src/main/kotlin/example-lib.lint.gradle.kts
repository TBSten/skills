// ktlint (ルールは .editorconfig が SSoT)。./gradlew ktlintCheck / ktlintFormat
plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set(libs.version("ktlint"))
    filter {
        // exclude("**/build/**") のようなパターンはソースディレクトリからの相対パスで照合されて効かないため、
        // 生成コード (KSP / kotest の launcher 等) は絶対パスで除外する
        exclude { it.file.absolutePath.contains("/build/") }
        exclude { it.file.absolutePath.contains("/generated/") }
    }
}
