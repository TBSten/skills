import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Maven Central publishing for every published module (`id("buildLogic.publish")`), so the
 * runtime and ksp modules cannot drift apart in coordinates, POM or signing.
 *
 * - artifactId is derived from the project path (`:<project-name>-ksp` -> `<project-name>-ksp`),
 *   so a new published module needs no per-module coordinates.
 * - the POM description comes from the module's own `description = "..."`.
 */
@Suppress("unused")
class PublishPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral()

                // Skip signing for local publishing so contributors do not need a GPG key. Compare
                // the LAST path segment: an exact match would miss `:<module>:publishToMavenLocal`
                // and then fail with "no configured signatory" on a machine without a key.
                val isLocalPublish =
                    gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "publishToMavenLocal" }
                if (!isLocalPublish) signAllPublications()

                coordinates(group.toString(), path.removePrefix(":").replace(":", "-"), version.toString())

                pom {
                    name.set(project.name)
                    description.set(provider { project.description ?: "<one-line description>" })
                    inceptionYear.set("<year>")
                    url.set("https://github.com/<owner>/<repo>/")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                            distribution.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("<owner>")
                            name.set("<owner>")
                            url.set("https://github.com/<owner>/")
                        }
                    }
                    scm {
                        url.set("https://github.com/<owner>/<repo>/")
                        connection.set("scm:git:git://github.com/<owner>/<repo>.git")
                        developerConnection.set("scm:git:ssh://git@github.com/<owner>/<repo>.git")
                    }
                }
            }
        }
    }
}
