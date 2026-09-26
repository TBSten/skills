import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
// @bootup:if kmp
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
// @bootup:end

// テストの共通構成。テストは kotest (FreeSpec + matcher) で書く。
// - KMP: commonTest に kotest-framework-engine、jvmTest に JUnit5 runner。
//   JS / Wasm / Native は example-lib.kmp が適用する KSP + kotest plugin が実行する
// - JVM: test に JUnit5 runner

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val kotestAssertions = libs.library("kotest-assertions-core")
// @bootup:if kmp
val kotestFrameworkEngine = libs.library("kotest-framework-engine")
// @bootup:end
val kotestRunnerJunit5 = libs.library("kotest-runner-junit5")

// @bootup:if kmp
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.matching { it.name == "commonTest" }.configureEach {
            dependencies {
                implementation(kotestFrameworkEngine)
                implementation(kotestAssertions)
            }
        }
        // JVM で動くテスト (jvm / Android host) は JUnit Platform + kotest runner で実行する
        sourceSets.matching { it.name == "jvmTest" || it.name == "androidHostTest" }.configureEach {
            dependencies {
                implementation(kotestRunnerJunit5)
            }
        }
    }
}

// @bootup:end
pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    dependencies {
        "testImplementation"(kotestRunnerJunit5)
        "testImplementation"(kotestAssertions)
    }
}
