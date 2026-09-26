# Build, toolchain, CI and publishing

Rationale and gotchas behind `example/`'s Gradle setup. The conventions that ship **into** the
generated project live in `assets/rules/`; this file is setup-time knowledge only.

## Version catalog is the single source of truth

`gradle/libs.versions.toml` holds every version — **including the project's own**:

```toml
[versions]
<project-name> = "0.1.0-alpha01"
```

The root build reads it back with `version = rootProject.libs.versions.<project-name>.get()` inside
`allprojects`, so a release is a one-line edit, `buildLogic` sees the same value, and `publish.yml`
can check the release tag against it.

**KSP versioning changed in 2.3.0.** Up to 2.2.x a KSP version was `<kotlin>-<ksp>`
(e.g. `2.2.20-2.0.4`) and had to move with Kotlin. From 2.3.0 it is independent (e.g. `2.3.11`) and
works across Kotlin versions. `scaffold.sh --kotlin-version` only rewrites the ksp entry when the
catalog still uses the old paired form.

## buildLogic as a plugin build

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("buildLogic")
    ...
}
```

Including it from `pluginManagement` (rather than a top-level `includeBuild`) is the form Gradle
documents for builds that only contribute plugins. `buildLogic/settings.gradle.kts` shares the root
catalog:

```kotlin
versionCatalogs {
    create("libs") { from(files("../gradle/libs.versions.toml")) }
}
```

**Plugin markers, not duplicate `[libraries]` entries.** A convention plugin needs the plugins it
applies on its compile classpath. Instead of listing each plugin's implementation artifact a second
time under `[libraries]`, `convention/build.gradle.kts` derives the plugin marker from `[plugins]`:

```kotlin
implementation(plugin(libs.plugins.vanniktech.mavenPublish))

fun plugin(plugin: Provider<PluginDependency>): Provider<String> =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
```

Markers resolve from the Gradle Plugin Portal, so `gradlePluginPortal()` must be in buildLogic's
`dependencyResolutionManagement.repositories`. Keep the Kotlin plugin marker there even though no
convention applies it directly: vanniktech 0.36+ reads Kotlin Gradle Plugin classes and otherwise
fails with "was not able to access Kotlin plugin classes". (With this included-build layout, modules
keep using `alias(libs.plugins.x)`; only a `buildSrc` layout forces `id(...)`.)

Put the **foojay resolver in both** `settings.gradle.kts` files. Without it in `buildLogic`,
`jvmToolchain(17)` inside the included build fails with "No matching toolchains" on a machine whose
default JDK differs.

Two conventions exist, each applied by at least two modules:

| Plugin | Applied by | Does |
|---|---|---|
| `buildLogic.lint` | every module | ktlint, excluding generated / build output |
| `buildLogic.publish` | runtime, ksp | vanniktech publishing, artifactId from path, POM, local-publish signing skip |

Add a third only when two modules genuinely need the same block. A snapshot-test convention is
deliberately **not** extracted: only the ksp module runs kctfork, so its test settings stay in
`<project-name>-ksp/build.gradle.kts`.

## gradle.properties

```properties
org.gradle.caching=true
org.gradle.configuration-cache=true
ksp.incremental=false
```

`ksp.incremental=false` is deliberate: a processor that reads across all annotated declarations
produces wrong output when an incremental round shows it only a subset. If your processor is strictly
per-file, you may re-enable it.

## Module shapes

| Module | Plugin | Why |
|---|---|---|
| `<project-name>-runtime` | `kotlinMultiplatform` + `androidKmpLibrary` + `buildLogic.publish` | Annotations only, every target, `explicitApi()`. Published |
| `<project-name>-ksp` | `kotlinJvm` + `buildLogic.publish` | KSP processors are JVM-only. `kspApi` + `kotlin("reflect")` + the runtime module. Uses context parameters (stable since Kotlin 2.4; add `-Xcontext-parameters` on 2.2 / 2.3). Published |
| `test` | `kotlinMultiplatform` + `androidKmpLibrary` + `ksp` + `kotest` | Applies the processor for real and asserts runtime behaviour on every target. Not published |

Register the provider by creating
`<project-name>-ksp/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
containing the provider's fully-qualified name (one line).

## Android: AGP 9 KMP library plugin

With AGP 9, a KMP library's Android target comes from `com.android.kotlin.multiplatform.library`
(catalog alias `androidKmpLibrary`), configured **inside** the `kotlin { }` block:

```kotlin
kotlin {
    android {
        namespace = "..."
        compileSdk = 36
        minSdk = 24
        withHostTest {} // only where Android unit tests are needed (the `test` module)
    }
}
```

What changes compared with `com.android.library` + `androidTarget()`:

- There is no top-level `android { }` block and no build variants; `publishLibraryVariants` is gone.
- Unit tests exist only after `withHostTest {}`. The source set is `androidHostTest` (was
  `androidUnitTest`) and the task is **`testAndroidHostTest`** (was `testDebugUnitTest`) — update the
  CI matrix together with the build file.
- x86_64 Apple targets (`macosX64`, `iosX64`, `watchosX64`, `tvosX64`) are deprecated by Kotlin and
  omitted from the runtime module.

## The KSP × KMP workaround

KSP does not process intermediate source sets such as `commonMain`. The fix, in the `test` module:

```kotlin
kotlin.sourceSets.commonMain { kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin") }

tasks.configureEach {
    if (name.startsWith("ksp") && name != "kspCommonMainKotlinMetadata") {
        dependsOn(tasks.named("kspCommonMainKotlinMetadata"))
        if (!name.contains("Test")) enabled = false
    }
}
```

Two details that are easy to get wrong:

- **Do NOT disable the `*Test` ksp tasks.** kotest's multiplatform framework is itself KSP-based and
  generates a per-target spec launcher; disabling those tasks makes specs silently not run on
  native/Android.
- **ktlint races the generator.** Make `runKtlint{Check,Format}OverCommonMainSourceSet` depend on
  `kspCommonMainKotlinMetadata`, and keep generated code out of ktlint. ktlint-gradle matches
  `exclude("**/build/generated/**")` against the path relative to the source directory, which never
  matches a srcDir that itself lives under `build/`; `buildLogic.lint` therefore excludes by absolute
  path (`exclude { it.file.invariantSeparatorsPath.contains("/build/") }`).

Also wire the processor only into `kspCommonMainMetadata` (plus the platform configurations you
actually need), never into a `*Test` KSP configuration — two processors on one source set is a
recipe for confusing failures.

## kotest wiring differs by module type

- **JVM-only module** (`<project-name>-ksp`): `kotest-runner-junit5` + `tasks.test { useJUnitPlatform() }`.
  No KSP, no `io.kotest` plugin.
- **KMP module** (`test`): apply the `io.kotest` plugin **after** `ksp`; `kotest-framework-engine` in
  `commonTest`; `kotest-runner-junit5` in **both** `jvmTest` and `androidHostTest` — the latter does
  not inherit the former, and omitting it means Android unit tests discover zero specs.

## The four `test` task settings for kctfork

```kotlin
tasks.named<Test>("test") {
    useJUnitPlatform()
    maxHeapSize = "2g"
    forkEvery = 25L
    providers.systemProperty("<project-name>.snapshot.update").orNull?.let {
        systemProperty("<project-name>.snapshot.update", it)
    }
}
```

`forkEvery` is not optional: each kctfork test creates classloaders that are never collected, so a
full suite exhausts the worker heap and the resulting OOM surfaces as failures in unrelated tests.
The `systemProperty` forwarding is likewise required — `-D` on the Gradle command line does not reach
the test worker JVM. Read it through `providers.systemProperty` so the configuration cache tracks it.
Goldens live under `src/test/resources/`, which is already a test input, so editing one re-runs the
tests without an extra `inputs.dir(...)`.

`.run/` ships IntelliJ run configurations for `:<project-name>-ksp:test` and its
`(update)` variant, so recording goldens is one click instead of remembering the `-D` flag.

## Which Kotlin can consumers use?

The runtime module is compiled with the catalog's Kotlin, and a compiler reads metadata at most one
minor version newer than itself. So consumers need **Kotlin ≥ (build Kotlin − 1 minor)**: building
with 2.4 serves 2.3+. The processor jar is only loaded by KSP (2.3+ is Kotlin-independent), so it
does not constrain consumers. State the minimum in the README.

A JVM-only library can serve older consumers by lowering `languageVersion` / `apiVersion` of its main
compilations plus `coreLibrariesVersion` (so the transitive stdlib is old too). This is **not**
applied here: for a KMP module, `coreLibrariesVersion` also pins the JS / Wasm stdlib, which then
fails with "standard library has the ABI version (2.2.0) that is not compatible with the compiler's
current ABI compatibility level (2.4)". If you need an older floor for the runtime module, build with
that older Kotlin instead.

## Binary compatibility validator

The root build applies `org.jetbrains.kotlinx.binary-compatibility-validator` with
`klib.enabled = true`, so both the JVM API and the klib ABI of the runtime module are frozen in
`<module>/api/*.api`. `apiCheck` runs in the CI `lint` job; after an intended API change run
`./gradlew apiDump` and commit the diff. `verify.sh --fresh` creates the first dump. Keep the `test`
module in `ignoredProjects` (scaffold.sh removes that line under `--skip-test-module`, because BCV
rejects unknown projects), and add any `@RequiresOptIn` internal marker to `nonPublicMarkers`.

## CI

`example/.github/workflows/gradle.yml`:

- `matrix.include` rather than a full axis product, so each target runs on the cheapest runner that
  can host it (only Apple targets need macOS).
- `concurrency` keyed on the PR number or ref, with `cancel-in-progress` **only for pull requests** —
  every commit on `main` is verified on its own.
- `gradle/actions/wrapper-validation` first in every job, then `setup-java` and
  `gradle/actions/setup-gradle` (the old `gradle/gradle-build-action` is deprecated).
- `~/.konan` is cached only in jobs that build native code, keyed on `gradle/libs.versions.toml`
  (where the Kotlin version lives). A key such as `hashFiles('**/.lock')` matches no file in a Gradle
  project and never invalidates.
- On failure, `build/reports` and `build/test-results` are uploaded as an artifact.
- Lint (`ktlintCheck apiCheck`) is its own job, so a formatting failure never hides a test failure.
- A final `final` job aggregates everything: branch protection requires that one check, and adding or
  renaming a matrix entry needs no settings change.
- `workflow_call` lets another workflow reuse CI as a gate.

## Publishing

`example/.github/workflows/publish.yml` triggers on `release: types: [published]`, which fires for
both full releases and pre-releases — including a pre-release published from a draft, where
`prereleased` does not fire and `released` never fires. Before publishing it checks that the release
tag (minus a leading `v`) equals the catalog's `<project-name>` version and that the version is not a
`-SNAPSHOT`. `concurrency: publish` with `cancel-in-progress: false` keeps two publishes from racing
without ever cancelling an upload in flight. It runs on macOS because only an Apple runner can build
every KMP target the runtime module publishes, and passes `--no-configuration-cache` because the
publish tasks are not configuration-cache compatible. CI and publish both use JDK 17.

`buildLogic.publish` derives the artifactId from the project path, takes the POM description from the
module's `description = "..."`, and skips signing for local publishing so contributors need no GPG
key. The check compares the **last task-path segment**, so `:<project-name>-ksp:publishToMavenLocal`
is recognised too:

```kotlin
val isLocalPublish =
    gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "publishToMavenLocal" }
if (!isLocalPublish) signAllPublications()
```

For the full Maven Central setup — GPG key generation, the five `ORG_GRADLE_PROJECT_*` secrets,
Sonatype Central Portal registration — use the **`kotlin-maven-central-publish`** skill rather than
repeating it here.

## Consider adding for a new project

Cheap at project start and painful to retrofit: Renovate or Dependabot, a `CONTRIBUTING.md`, and
issue/PR templates.
