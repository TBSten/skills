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
`allprojects`, so a release is a one-line edit and `buildLogic` sees the same value.

**KSP versions are always `<kotlin>-<ksp>`** (e.g. `2.2.20-2.0.4`). Bumping Kotlin without bumping
KSP fails with a version-mismatch error at configuration time — treat them as one edit.

## buildLogic as an included build

`includeBuild("./buildLogic")` plus, in `buildLogic/settings.gradle.kts`:

```kotlin
versionCatalogs {
    create("libs") { from(files("../gradle/libs.versions.toml")) }
}
```

That sharing is the whole point — otherwise convention plugins pin their own versions and drift.

Put the **foojay resolver in both** `settings.gradle.kts` files. Without it in `buildLogic`,
`jvmToolchain(17)` inside the included build fails with "No matching toolchains" on a machine whose
default JDK differs.

Start the convention layer with lint alone (`buildLogic.lint`). Add a second convention plugin only
when two modules genuinely need the same block; a speculative `kotlin-jvm` convention that one module
uses is worse than no convention.

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
| `<project-name>-runtime` | `kotlinMultiplatform` + `androidLibrary` | Annotations only, every target, `explicitApi()`. Published |
| `<project-name>-ksp` | `kotlinJvm` | KSP processors are JVM-only. `kspApi` + `kotlin("reflect")` + the runtime module. Needs `-Xcontext-parameters`. Published |
| `test` | `kotlinMultiplatform` + `ksp` + `kotest` | Applies the processor for real and asserts runtime behaviour on every target. Not published |

Register the provider by creating
`<project-name>-ksp/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
containing the provider's fully-qualified name (one line).

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
  `kspCommonMainKotlinMetadata`, and exclude `**/build/generated/**` from ktlint.

Also wire the processor only into `kspCommonMainMetadata` (plus the platform configurations you
actually need), never into a `*Test` KSP configuration — two processors on one source set is a
recipe for confusing failures.

## kotest wiring differs by module type

- **JVM-only module** (`<project-name>-ksp`): `kotest-runner-junit5` + `tasks.test { useJUnitPlatform() }`.
  No KSP, no `io.kotest` plugin.
- **KMP module** (`test`): apply the `io.kotest` plugin **after** `ksp`; `kotest-framework-engine` in
  `commonTest`; `kotest-runner-junit5` in **both** `jvmTest` and `androidUnitTest` — the latter does
  not inherit the former, and omitting it means Android unit tests discover zero specs.

## The four `test` task settings for kctfork

```kotlin
tasks.named<Test>("test") {
    useJUnitPlatform()
    maxHeapSize = "2g"
    forkEvery = 25L
    System.getProperty("<project-name>.snapshot.update")?.let {
        systemProperty("<project-name>.snapshot.update", it)
    }
}
```

`forkEvery` is not optional: each kctfork test creates classloaders that are never collected, so a
full suite exhausts the worker heap and the resulting OOM surfaces as failures in unrelated tests.
The `systemProperty` forwarding is likewise required — `-D` on the Gradle command line does not reach
the test worker JVM.

## CI

Use a `matrix.include` list rather than a full axis product, so each target runs on the cheapest
runner that can host it (only Apple targets need macOS). Give every job `timeout-minutes`, add
`concurrency` with `cancel-in-progress`, and run `gradle/actions/wrapper-validation` as the first
step of every job — a tampered wrapper would otherwise execute before any check.

Keep lint as its own job: a formatting failure should not hide a real test failure.

## Publishing

`example/.github/workflows/publish.yml` triggers on `release: types: [published]`, which fires for
both full releases and pre-releases — including a pre-release published from a draft, where
`prereleased` does not fire and `released` never fires. It runs on macOS because only an Apple runner
can build every KMP target the runtime module publishes, and passes
`--no-configuration-cache` because the publish tasks are not configuration-cache compatible.

In the module build files, skip signing for local publishing so contributors need no GPG key:

```kotlin
if (!gradle.startParameter.taskNames.contains("publishToMavenLocal")) {
    signAllPublications()
}
```

For the full Maven Central setup — GPG key generation, the five `ORG_GRADLE_PROJECT_*` secrets,
Sonatype Central Portal registration — use the **`kotlin-maven-central-publish`** skill rather than
repeating it here.

## Consider adding for a new project

The example intentionally does not include these, but they are cheap at project start and painful to
retrofit: `binary-compatibility-validator` (beyond `explicitApi()`), Renovate or Dependabot, a
`CONTRIBUTING.md`, and issue/PR templates. Also decide deliberately whether CI and publish run on the
same JDK; if they differ, write down why.
