package com.example.compilerpluginsetup.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

/**
 * Checks the consumer's Kotlin Gradle plugin version against the supported range
 * (`SUPPORTED_KOTLIN_MIN` / `SUPPORTED_KOTLIN_MAX_TESTED_EXCLUSIVE` in gradle.properties):
 *
 * - `< MIN` -> [GradleException]. Compiler APIs differ too much; failing here beats a
 *   `NoSuchMethodError` deep inside the compiler.
 * - `>= MAX_TESTED_EXCLUSIVE` -> warning only (not verified yet, but may work).
 * - Unknown / unparsable version -> warning, guard skipped.
 */
internal object ExampleKotlinVersionGuard {

    fun check(project: Project) {
        val raw = runCatching { project.getKotlinPluginVersion() }.getOrNull()
        val current = raw?.let(KotlinVersionParts::parse)
        if (current == null) {
            project.logger.warn("[example-plugin] Could not detect the Kotlin version ('$raw'). Version guard skipped.")
            return
        }
        val min = requireNotNull(KotlinVersionParts.parse(BuildConfig.SUPPORTED_KOTLIN_MIN))
        val maxExclusive = requireNotNull(KotlinVersionParts.parse(BuildConfig.SUPPORTED_KOTLIN_MAX_TESTED_EXCLUSIVE))
        if (current < min) {
            throw GradleException(
                "[example-plugin] requires Kotlin ${BuildConfig.SUPPORTED_KOTLIN_MIN} or later, " +
                    "but ${project.path} uses Kotlin $raw. Upgrade the Kotlin Gradle plugin.",
            )
        }
        if (current >= maxExclusive) {
            project.logger.warn(
                "[example-plugin] Kotlin $raw is newer than the latest verified version " +
                    "(< ${BuildConfig.SUPPORTED_KOTLIN_MAX_TESTED_EXCLUSIVE}). It may still work, but is not supported yet.",
            )
        }
    }
}

/**
 * Minimal Kotlin version comparator (`2.3.10`, `2.4.0-Beta2`, `2.4.0-RC`).
 * A pre-release is smaller than the stable release with the same base (`2.4.0-RC < 2.4.0`).
 */
internal data class KotlinVersionParts(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val classifier: String?,
) : Comparable<KotlinVersionParts> {

    override fun compareTo(other: KotlinVersionParts): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }).takeIf { it != 0 }
            ?: when {
                classifier == other.classifier -> 0
                classifier == null -> 1
                other.classifier == null -> -1
                else -> classifier.compareTo(other.classifier)
            }

    companion object {
        fun parse(raw: String): KotlinVersionParts? {
            val parts = raw.substringBefore('-').split('.')
            if (parts.size != 3) return null
            return KotlinVersionParts(
                major = parts[0].toIntOrNull() ?: return null,
                minor = parts[1].toIntOrNull() ?: return null,
                patch = parts[2].toIntOrNull() ?: return null,
                classifier = raw.substringAfter('-', missingDelimiterValue = "").ifEmpty { null },
            )
        }
    }
}
