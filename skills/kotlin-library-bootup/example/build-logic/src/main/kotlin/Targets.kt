// @bootup:file-if kmp
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * example-lib.kmp が適用する KMP ターゲット一式。
 *
 * NOTE: `android { }` は `com.android.kotlin.multiplatform.library` が動的に登録する extension で、
 * 素の `.kt` ファイルには accessor が生成されない。そのため extensions.configure で明示的に設定する。
 */
internal fun KotlinMultiplatformExtension.configureExampleLibTargets(project: Project) {
    val libs = project.libs
    val jvmTarget = JvmTarget.fromTarget(libs.version("jvmToolchain"))

    explicitApi()
    jvmToolchain(libs.version("jvmToolchain").toInt())

    extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
        // モジュール名から namespace を導出する (例: example-lib-core -> <package>.example_lib_core)
        namespace = "com.example.kotlinlibrarybootup." + project.name.replace("-", "_")
        compileSdk = libs.version("androidCompileSdk").toInt()
        minSdk = libs.version("androidMinSdk").toInt()
        // commonTest を Android (JVM host) でも回す: ./gradlew testAndroidHostTest
        withHostTest {}
        compilerOptions { this.jvmTarget = jvmTarget }
    }

    jvm {
        compilerOptions { this.jvmTarget = jvmTarget }
    }
    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    if (project.appleTargetsEnabled()) {
        iosArm64()
        iosSimulatorArm64()
        // @bootup:if full
        //~ // CI でテストする: macosArm64Test (macOS runner)
        //~ macosArm64()
        //~ // 以下はコンパイル・publish のみ (CI ではテストしない)
        //~ watchosArm64()
        //~ watchosDeviceArm64()
        //~ watchosSimulatorArm64()
        //~ tvosArm64()
        //~ tvosSimulatorArm64()
        //~ // NOTE: x64 系 Apple ターゲット (iosX64 / macosX64 / tvosX64 / watchosX64) は Kotlin 2.x で
        //~ // 非推奨化が進んでいるため含めない。必要なら利用者の要望を見て追加する。
        // @bootup:end
    }
    // @bootup:if full
    //~ // CI でテストする: linuxX64Test (ubuntu runner)
    //~ linuxX64()
    //~ // 以下はコンパイル・publish のみ (CI ではテストしない。クロスコンパイルで klib を出す)
    //~ linuxArm64()
    //~ mingwX64()
    //~ androidNativeArm32()
    //~ androidNativeArm64()
    //~ androidNativeX86()
    //~ androidNativeX64()
    //~ @OptIn(ExperimentalWasmDsl::class)
    //~ wasmWasi {
    //~     nodejs()
    //~ }
    // @bootup:end

    compilerOptions {
        configureCommon(project)
    }
    configureCompatibilityFloor(project)

    // JS / Wasm の stdlib (klib) はコンパイラと同じ ABI 版でないとコンパイルできない
    // ("The Kotlin/JS standard library has the ABI version (2.2.0) that is not compatible ...")。
    // そのため JS / Wasm のコンパイル時だけ stdlib をコンパイラ版に解決させる。
    // 公開 metadata 上の依存宣言は床 (kotlinCoreLibrariesVersion) のままなので、利用側は自分の版を使う。
    val compilerVersion = libs.version("kotlin")
    val webPrefixes = listOf("js", "wasmJs", "wasmWasi", "web")
    project.configurations
        .matching { c -> webPrefixes.any { c.name.startsWith(it) } }
        .configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
                    useVersion(compilerVersion)
                }
            }
        }
}

/**
 * Apple (iOS 等) ターゲットを宣言するか。
 *
 * - `-PenableAppleTargets=true`  : 常に有効 (publish.yml はこれを付けて Xcode 必須を明示する)
 * - `-PdisableAppleTargets=true` : 常に無効 (Xcode はあるが Apple ビルドを省きたい時)
 * - どちらも無い時:
 *   - macOS: Xcode (xcode-select -p が *.app を指す) がある時だけ有効。
 *     CommandLineTools だけの環境では link が `xcrun exit code 72` で落ちるため無効にする。
 *   - Linux / Windows: 有効。Kotlin Gradle Plugin がホスト非対応ターゲットのタスクを自動で skip する。
 */
internal fun Project.appleTargetsEnabled(): Boolean {
    if (providers.gradleProperty("enableAppleTargets").orNull?.toBoolean() == true) return true
    if (providers.gradleProperty("disableAppleTargets").orNull?.toBoolean() == true) return false
    val isMac = providers.systemProperty("os.name").get().startsWith("Mac")
    if (!isMac) return true
    val developerDir = providers
        .exec {
            commandLine("xcode-select", "-p")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
    return developerDir.contains(".app/")
}
