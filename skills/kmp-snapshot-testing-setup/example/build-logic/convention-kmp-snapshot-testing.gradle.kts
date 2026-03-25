import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * JVM スナップショットテスト用 convention plugin.
 * jvmSnapshotTestRecord / jvmSnapshotTestVerify / snapshotReport タスクを登録する。
 *
 * convention-kmp 適用後に使用すること。
 *
 * カスタマイズが必要な箇所:
 * - kotest.framework.config.fqn: プロジェクトの ProjectConfig の FQCN に変更
 * - :core:testing:snapshot: テスト基盤モジュールのパスに変更
 */
plugins {
    id("org.jetbrains.kotlin.plugin.serialization")
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

    kotlin.jvm {
        val main = compilations.getByName("main")

        compilations.create("snapshotTest") {
            associateWith(main)

            defaultSourceSet {
                dependencies {
                    implementation(kotlin("test"))
                    implementation(project.dependencies.platform("org.junit:junit-bom:5.10.2"))
                    implementation("org.junit.platform:junit-platform-engine")
                    // TODO: テスト基盤モジュールのパスに変更
                    implementation(rootProject.project(":core:testing:snapshot"))
                }
            }
        }
    }

    val snapshotCompilation =
        kotlin.targets.getByName("jvm")
            .compilations.getByName("snapshotTest")

    val baseJvmTest = tasks.named<Test>("jvmTest")

    val snapshotOutputDir = provider {
        rootProject.layout.projectDirectory.dir("build/snapshots")
    }

    fun registerSnapshotTestTask(flavor: String) {
        tasks.register<Test>("jvmSnapshotTest${flavor.replaceFirstChar { it.uppercase() }}") {
            val base = baseJvmTest.get()

            group = "verification"
            description = "Runs JVM snapshot tests ($flavor)"
            useJUnitPlatform()

            systemProperties(base.systemProperties)
            jvmArgs(base.jvmArgs)

            systemProperty("snapshot-test-flavor", flavor)
            // TODO: プロジェクトの ProjectConfig の FQCN に変更
            systemProperty(
                "kotest.framework.config.fqn",
                "<your-package>.ProjectConfig",
            )
            val outputDir = snapshotOutputDir
                .get().asFile
                .toString()

            systemProperty("snapshot-test-output-dir", outputDir)

            project.findProperty("pbt.iteration.count")?.let {
                systemProperty("pbt.iteration.count", it.toString())
            }

            testLogging {
                events = base.testLogging.events
                exceptionFormat = base.testLogging.exceptionFormat
                showExceptions = base.testLogging.showExceptions
                showCauses = base.testLogging.showCauses
                showStackTraces = base.testLogging.showStackTraces
                showStandardStreams = base.testLogging.showStandardStreams
            }

            testClassesDirs = snapshotCompilation.output.classesDirs
            classpath = files(
                snapshotCompilation.output.allOutputs,
                snapshotCompilation.runtimeDependencyFiles,
            )

            dependsOn(snapshotCompilation.compileTaskProvider)

            if (flavor == "record") {
                dependsOn(rootProject.tasks.named("cleanSnapshotOutputDir"))
            }
        }
    }

    registerSnapshotTestTask("record")
    registerSnapshotTestTask("verify")

    tasks.register<SnapshotReportTask>("snapshotReport") {
        group = "verification"
        description = "Generates snapshot diff report (result.json + result.md)"
        snapshotDirPath.set(
            providers.gradleProperty("snapshotReport.dir")
                .orElse(snapshotOutputDir.map { it.asFile.absolutePath })
        )
        baselineManifestPath.set(providers.gradleProperty("snapshotReport.baselineManifest"))
        beforeRef.set(providers.gradleProperty("snapshotReport.before").orElse("main"))
        afterRef.set(
            providers.gradleProperty("snapshotReport.after").orElse("current working tree")
        )
        pbtIteration.set(providers.gradleProperty("snapshotReport.pbtIteration"))
    }
}
