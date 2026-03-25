import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * KMP テスト依存を追加する convention plugin.
 * commonTest: Kotest, Turbine, coroutines-test
 * jvmTest: Kotest JUnit5 runner, HTML/XML reporter
 *
 * convention-kmp 適用後に使用すること。
 */

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets {
            named("commonTest") {
                dependencies {
                    implementation(kotlin("test"))
                    implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                    implementation(libs.findLibrary("kotestFrameworkEngine").get())
                    implementation(libs.findLibrary("kotestAssertionsCore").get())
                    implementation(libs.findLibrary("turbine").get())
                }
            }
            named("jvmTest") {
                dependencies {
                    implementation(libs.findLibrary("kotestRunnerJunit5").get())
                    implementation(libs.findLibrary("kotestExtensionsHtmlReporter").get())
                    implementation(libs.findLibrary("kotestExtensionsJunitXml").get())
                }
            }
        }
    }
}
