plugins {
    `kotlin-dsl`
}

kotlin {
    // 起動した JDK に依存せず、provision された JDK でコンパイルする
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

// precompiled script plugin (src/main/kotlin/*.gradle.kts) から各プラグインの DSL を
// 参照できるように、plugin marker (<id>:<id>.gradle.plugin:<version>) を implementation に載せる。
// バージョンは version catalog (gradle/libs.versions.toml) が SSoT。
dependencies {
    // @bootup:if kmp
    implementation(plugin(libs.plugins.kotlin.multiplatform))
    implementation(plugin(libs.plugins.android.kmp.library))
    implementation(plugin(libs.plugins.ksp))
    implementation(plugin(libs.plugins.kotest))
    // @bootup:end
    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.maven.publish))
    implementation(plugin(libs.plugins.dokka))
    implementation(plugin(libs.plugins.ktlint))
}

fun plugin(plugin: Provider<PluginDependency>): Provider<String> = plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
