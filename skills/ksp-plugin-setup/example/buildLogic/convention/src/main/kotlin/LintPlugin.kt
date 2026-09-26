import dsl.alias
import dsl.ktlint
import dsl.libs
import dsl.plugin
import dsl.version
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The one convention plugin every module applies (`id("buildLogic.lint")`).
 *
 * Start the convention layer minimal — lint only — and grow it only when a second module needs the
 * same block. Generated sources and build outputs are excluded because ktlint would otherwise fail
 * on code this project does not own.
 */
@Suppress("unused")
class LintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                alias(libs.plugin("ktlintGradle"))
            }

            ktlint {
                version.set(libs.version("ktlint"))

                filter {
                    // ktlint-gradle matches glob patterns against the path RELATIVE to the source
                    // directory, so `**/build/generated/**` never matches a srcDir that itself lives
                    // under build/ (the KSP output added to commonMain). Match the absolute path.
                    exclude { it.file.invariantSeparatorsPath.contains("/build/") }
                }
            }
        }
    }
}
