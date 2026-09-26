import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// テストの共通構成。
// テストは kotlin.test (@Test) + kotest-assertions (shouldBe 等) で書く。
// kotest の spec (FunSpec 等) を JS / Wasm / Native で動かすには kotest Gradle plugin + KSP が
// 必要になるため、全ターゲット共通で動くこの組み合わせを既定にしている。

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val kotestAssertions = libs.library("kotest-assertions-core")

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.getByName("commonTest").dependencies {
            implementation(kotlin("test"))
            implementation(kotestAssertions)
        }
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    dependencies {
        "testImplementation"(kotlin("test"))
        "testImplementation"(kotestAssertions)
    }
}
