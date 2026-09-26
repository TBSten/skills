import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// precompiled script plugin には version catalog の型安全 accessor が生成されないため、
// 名前で引く薄いヘルパーを用意する。キーが無ければ設定時に即失敗させる。

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(name: String): String = findVersion(name)
    .orElseThrow { IllegalStateException("gradle/libs.versions.toml に [versions] $name が無い") }
    .requiredVersion

internal fun VersionCatalog.library(name: String): MinimalExternalModuleDependency = findLibrary(name)
    .orElseThrow { IllegalStateException("gradle/libs.versions.toml に [libraries] $name が無い") }
    .get()
