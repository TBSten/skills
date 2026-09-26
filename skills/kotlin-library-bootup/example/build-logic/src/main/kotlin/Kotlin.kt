import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * opt-in 必須マーカー。自モジュール内では opt-in 済みとして扱い、利用側にだけ opt-in を要求する。
 * root build.gradle.kts の apiValidation.nonPublicMarkers と揃える。
 */
internal val OPT_IN_MARKERS: List<String> = listOf(
    "com.example.kotlinlibrarybootup.InternalExampleLibApi",
    "com.example.kotlinlibrarybootup.ExperimentalExampleLibApi",
)

/**
 * 公開する成果物を「床」の Kotlin でも読める metadata で出す (下位互換の床)。
 *
 * 既定のままだと metadata はコンパイラ (2.4) 基準になり、Kotlin 2.2 のプロジェクトでは
 * このライブラリのシンボルが Unresolved reference になる。languageVersion / apiVersion を下げ、
 * 推移的に入る kotlin-stdlib (coreLibrariesVersion) も同じ床に揃える。
 * 代償として、床より新しい言語機能・stdlib API は使えない。
 */
internal fun KotlinBaseExtension.configureCompatibilityFloor(project: Project) {
    coreLibrariesVersion = project.libs.version("kotlinCoreLibrariesVersion")
}

internal fun KotlinCommonCompilerOptions.configureCommon(project: Project) {
    val floor = KotlinVersion.fromVersion(project.libs.version("kotlinLanguageVersion"))
    languageVersion.set(floor)
    apiVersion.set(floor)
    optIn.addAll(OPT_IN_MARKERS)
}
