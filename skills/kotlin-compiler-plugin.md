# Kotlin Compiler Plugin Skill

[日本語](./kotlin-compiler-plugin.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill for Kotlin K2 Compiler Plugins (FIR + IR): setting up a new project, designing / reviewing / researching precedents, adding or removing supported Kotlin versions, and scaffolding cross-module compile-time collection.

It merges the former `kotlin-compiler-plugin-setup`, `kotlin-compiler-plugin-dev` and `collector-compiler-plugin` skills. `SKILL.md` only routes the request to one mode, and each mode's guide lives in `references/<mode>/guide.md`, so the agent loads only what the task needs.

## Quick Start

### 1. Install the skill:

```bash
gh skill install tbsten/skills kotlin-compiler-plugin
```

### 2. Ask your AI agent:

```
Set up a Kotlin compiler plugin project.
Should this transformation be done in FIR or IR? Any existing plugin doing something similar?
Add Kotlin 2.4.0 support to our compiler plugin.
I want to collect classes annotated with @Collectable across modules at compile time.
```

## Modes

| Mode | Use it for | Guide | Formerly |
|---|---|---|---|
| [setup](#setup-mode) | Scaffold a new multi-module compiler plugin project (compiler-plugin / gradle-plugin / runtime / kctfork tests / integration tests / CI and release) | `references/setup/guide.md` | `kotlin-compiler-plugin-setup` |
| [dev](#dev-mode) | Design advice (FIR vs IR, Extension Points), precedent research, implementation review, adding / removing supported Kotlin versions | `references/dev/guide.md` | `kotlin-compiler-plugin-dev` |
| [collector](#collector-mode) | Collect annotated classes across Gradle modules at compile time (hint function pattern) | `references/collector/guide.md` | `collector-compiler-plugin` |

## Skill Layout

```
kotlin-compiler-plugin/
├── SKILL.md                 # routes to a mode
├── references/
│   ├── setup/               # setup guide + design notes
│   ├── dev/                 # dev guide + precedent research data + multi-version patterns
│   └── collector/           # collector guide + architecture + code templates
├── scripts/scaffold.sh      # setup: generates the project from example/
├── example/                 # setup: complete project skeleton
├── assets/                  # dev: test infrastructure copied into target projects
└── evals/
```

## Setup Mode

### How It Works

The skill runs `scripts/scaffold.sh`, which copies the complete project skeleton from `example/` — build files plus ready-made Kotlin sources (CommandLineProcessor, CompilerPluginRegistrar, IR/FIR extensions, a no-op transformer, a kctfork test helper, and integration-test entry points), written with the real package `com.example.compilerpluginsetup` and an `Example` class prefix — and replaces/renames everything to your project name, package, group ID, and plugin ID. The agent executes the script as-is (no manual transcription of code from docs), reviews the generated files, and verifies the build.

### What Gets Set Up

#### Project Structure

| Module | Description |
|---|---|
| `buildSrc/` | Convention plugins: `kotlin-jvm` (JDK 21 toolchain, Java 17 bytecode, JUnit5 + test logging) and `publish` (vanniktech maven-publish, signing only when a key is present) |
| `compiler-plugin/` | Compiler plugin with AutoService + KSP, FIR + IR extensions, an `enabled` CLI option wired end to end |
| `gradle-plugin/` | Gradle plugin wrapper (KotlinCompilerPluginSupportPlugin) with a `Property` + `convention()` DSL, fail-fast guard, consumer Kotlin version guard and a generated `BuildConfig` |
| `runtime/` | Kotlin Multiplatform API declarations (one placeholder marker) |
| `integration-test/test-jvm/` | JVM end-to-end test with `kotlinCompilerPluginClasspath` |
| `integration-test/test-kmp/` | KMP (JVM + JS) end-to-end test with `kotlinCompilerPluginClasspath` |
| `integration-test/test-gradle-plugin/` | Gradle TestKit E2E: a fixture build applies `plugins { id(...) }` and resolves everything via `includeBuild` + `dependencySubstitution` |
| `.github/workflows/` | `ci.yml` (PR-only cancel, paths-ignore, reports on failure, `final` fan-in job) and `release.yml` (tag push, tag == version check, Maven Central, GitHub Release) |

#### Build Configuration

| File | Description |
|---|---|
| `gradle.properties` | SSoT for `GROUP` / `VERSION_NAME` / plugin ID / supported Kotlin range / `POM_*`, plus build speed settings (caching, configuration cache, parallel, KT-82395 workaround) |
| `build.gradle.kts` | `allprojects { group; version }` from gradle.properties |
| `settings.gradle.kts` | Multi-module project with foojay toolchain resolver |
| `gradle/libs.versions.toml` | Version catalog (Kotlin, KSP, AutoService, kctfork, Kotest, maven-publish) |
| `buildSrc/build.gradle.kts` | kotlin-dsl with shared version catalog |

#### Testing Infrastructure

| Component | Description |
|---|---|
| kctfork (KotlinCompilation) | In-memory compilation for unit tests |
| Kotest (FunSpec) | Test framework with JUnit5 runner |
| Integration test (JVM / KMP) | Modules with `kotlinCompilerPluginClasspath` |
| Gradle plugin sanity | `ProjectBuilder` tests for DSL defaults and runtime dependency wiring |
| Gradle plugin E2E | Gradle TestKit + fixture project (a consumer-style build, no publishing needed) |

### Key Concepts

#### Plugin Registration

Compiler plugins register via `META-INF/services/` files, auto-generated by `@AutoService`:
- `CommandLineProcessor` — declares plugin ID
- `CompilerPluginRegistrar` — registers FIR + IR extensions

#### FIR vs IR

- **FIR (Frontend)** — validation and early error reporting with accurate line numbers
- **IR (Backend)** — actual code transformation affecting bytecode/JS/Native output

#### Unit Test Pattern

Uses kctfork to compile Kotlin source in-memory and verify results via reflection:
- `compile(source)` — compiles with plugin registered
- `shouldCompileOk()` — asserts successful compilation
- `loadTopLevelField(name)` — loads field value via classloader

### Related

- [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) (MIT, by kitakkun) — after scaffolding, the skill points to its guides for bootstrap, official test infrastructure (diagnostic / box tests, complementary to kctfork), Gradle plugin integration, and multi-version support. Referenced by raw URL (not copied); a locally installed copy is preferred
- [Dev mode](#dev-mode) — precedent research across 30+ plugins, the full kitakkun topic map for FIR/IR extensions, and adding/removing supported Kotlin versions

## Dev Mode

### What it does

This skill helps you:

1. **Find precedents** — Search 30+ existing compiler plugins to find implementations similar to what you want to build
2. **Choose Extension Points** — Determine the right FIR/IR Extension Points based on your requirements
3. **Review implementations** — Evaluate your compiler plugin against established patterns and best practices
4. **Review multi-version support** — Evaluate compat module layer / source set separation architecture; advise on tandem vs. independent release strategy
5. **Add/remove supported Kotlin versions** — Drive compat module additions (delegation pattern), capability flags, reflection shims, SSOT-driven CI matrix, kctfork version mapping, and Java 21 toolchain pinning
6. **Deep-dive into source code** — Use deepwiki MCP to read the latest source code of reference plugins

### When to use

- Starting a new compiler plugin project
- Adding a new feature to an existing compiler plugin
- Reviewing a compiler plugin implementation for correctness
- Researching how existing plugins implement a specific feature
- Deciding between FIR and IR for a particular transformation
- Adding or removing a supported Kotlin version on a project that already has multi-version infrastructure

### How it works

1. **Step 0**: Checks if deepwiki MCP is available (optional, enhances source code exploration)
2. **Step 1**: Understands your requirements (develop / review / research / add-or-remove supported Kotlin version)
3. **Step 2**: Searches `references/dev/overview.md` — a table of 130+ Extension Point implementations across 30 plugins
4. **Step 3**: Reads `references/dev/details/` files for in-depth implementation details (inheritance, overrides, behavior, diagnostics)
5. **Step 4**: Optionally uses deepwiki MCP to fetch the latest source code from GitHub
6. **Step 5**: Outputs a design proposal, review report, or research summary
7. **Step 6**: For add/remove supported Kotlin version mode, follows `references/dev/multi-version-workflow.md` to add a compat module, update the SSOT / CI matrix, and verify per-version tests

### Bundled references

| File | Description |
|---|---|
| `references/dev/overview.md` | Flat table of all Extension Points across 30+ plugins with source URLs |
| `references/dev/patterns.md` | 4 architectural patterns + Extension Point selection guides + Multi-Version Support architectures (compat module layer / source set separation) |
| `references/dev/review-checklist.md` | Checklists for K2 compatibility, design patterns, code quality, multi-Kotlin-version support |
| `references/dev/details/*.md` | Source-code-level details for each plugin group (8 files) |
| `references/dev/multi-version-workflow.md` | Detailed workflow for adding/removing supported Kotlin versions |
| `references/dev/compat-module-setup.md` | `CompatContext` SPI / delegation pattern / ShadowJar packaging |
| `references/dev/source-set-separation.md` | Source set separation approach in depth |
| `references/dev/ci-matrix.md` | SSOT-driven dynamic matrix YAML template + per-version test script |
| `references/dev/kotlin-tooling-version.md` | `KotlinToolingVersion` with Maturity-aware comparison |
| `references/dev/version-gating.md` | Capability flag design + test-side self-skip |
| `references/dev/reflection-shim.md` | Reflection shim to absorb small API drift without a new compat module |
| `references/dev/troubleshooting.md` | Failure pattern → root cause → remediation table |
| `references/dev/release-operations.md` | Operations: mavenLocal smoke test of published artifacts (`javap`), local parallel CI matrix, semver policy (adding a Kotlin version = MINOR, dropping = MAJOR), project-local skills / `.claude/rules` with `paths`, test-strategy matrix |
| `references/dev/external-kitakkun-skills.md` | Topic map (24 guides, raw URLs, install steps) for the external kitakkun/kotlin-compiler-plugin-skills guides, and how to combine them with this skill's references |

### Bundled assets

Ready-to-copy files for target projects that lack the multi-version test infrastructure (the skill copies them as-is instead of hand-writing equivalents):

| File | Copy to | Description |
|---|---|---|
| `assets/scripts/compiler-plugin-test.sh` | `scripts/` (+ `chmod +x`) | Per-version test runner; `--all` loops over the SSOT and reports failing versions |
| `assets/scripts/supported-kotlin-versions.txt` | `scripts/` | SSOT template for supported Kotlin versions (edit to match the project) |
| `assets/workflows/compiler-plugin-test.yml` | `.github/workflows/` | SSOT-driven dynamic CI matrix (resolve + test jobs) |
| `assets/scripts/smoke-test.sh` | `scripts/` (+ `chmod +x`) | Optional. publishToMavenLocal → build an independent consumer with a given Kotlin version → verify injected symbols with `javap -p -c` (`--absent` checks zero overhead) |

### Prerequisites

- A Kotlin project with compiler plugin source code (or a plan to create one)
- For supported-version add/remove, the project must already have multi-version infrastructure (compat module layer or source set separation). For initial setup, see [Setup mode](#setup-mode) (`references/setup/guide.md` Step 4: Multi-Kotlin Version Support)
- Optional: deepwiki MCP server configured for enhanced source code exploration

### Related

- [kitakkun/kotlin-compiler-plugin-skills](https://github.com/kitakkun/kotlin-compiler-plugin-skills) (MIT, by kitakkun) — per-topic guides for FIR/IR extension APIs, official test infrastructure, debugging, Gradle integration, and multi-version support (Kotlin 2.4.x, verified against the Kotlin compiler source). This skill uses it for precedent research → API how-to: this skill's `references/dev/details/` answers "which existing plugin does this", the kitakkun guides answer "how to write it with the current API". Referenced by link (not copied); see `references/dev/external-kitakkun-skills.md`

## Collector Mode

> The collector mode was originally the `collector-compiler-plugin` skill, moved from [TBSten/collector-compiler-plugin-skill](https://github.com/TBSten/collector-compiler-plugin-skill) and is now maintained in this repository.

### Background

[metro](https://github.com/ZacSweers/metro) and [koin-compiler-plugin](https://github.com/InsertKoinIO/koin-compiler-plugin) both use a Kotlin Compiler Plugin to discover and aggregate annotated classes across multiple modules at compile time. The mechanism is useful beyond DI frameworks, but implementing a compiler plugin has a high barrier to entry. This skill generalizes the pattern so you can add the same mechanism to your own project. It covers cross-module collection that KSP cannot do.

### How It Works

1. **Hearing** — project type (KMP / Android / JVM), what to collect, tag grouping, base package, annotation names
2. **Design review** — annotation names/arguments, hint package (`<base-package>.hints`), module layout, generated file list
3. **Code generation** — reads `references/collector/architecture.md` and customizes `references/collector/templates/*` for your package and annotation names
4. **Verification guide** — `./gradlew :compiler-plugin:test`, `publishToMavenLocal`, then try it in a sample project

#### Generated Modules

| Module | Description |
|---|---|
| `annotations/` | `@Collectable` / `@CollectAll` marker annotations (KMP, runtime dependency) |
| `compiler-plugin/` | `CompilerPluginRegistrar` + FIR / IR extensions (JVM, compile-time only) |
| `gradle-plugin/` | Gradle plugin that applies the compiler plugin |

Tests use `kotlin-compile-testing`.

#### User-facing API

```kotlin
@Collectable(tag = "api-handler")
class UserApiHandler : ApiHandler { ... }

// The compiler generates the body
@CollectAll(tag = "api-handler")
fun collectApiHandlers(): List<KClass<*>>
```

### Key Concepts

#### Hint Function Pattern

```
[Compiling Module A]
  1. FIR: detect @Collectable classes → generate hint function stubs
  2. IR: generate hint bodies → expose via registerFunctionAsMetadataVisible()
  → hint functions are included in the JAR metadata

[Compiling Module B (depends on Module A)]
  3. Look up the hint package with referenceFunctions(CallableId)
  4. Restore the collected classes from the hint functions' parameter types
```

| API | Role |
|---|---|
| `CompilerPluginRegistrar` | Entry point |
| `FirDeclarationGenerationExtension` | Generates hint function stubs in FIR |
| `IrGenerationExtension` | Generates hint bodies + discovery in IR |
| `registerFunctionAsMetadataVisible()` | Exposes hint functions to downstream modules |
| `referenceFunctions(CallableId)` | Looks up hint functions downstream |

#### metro vs koin

| Aspect | metro | koin |
|---|---|---|
| Hint package | `metro.hints` | `org.koin.plugin.hints` |
| Function name | Derived from scope name (`appScope`) | prefix + metadata (`componentscan_*_single`) |
| Metadata storage | Parameter types only | Function name + parameter names/types |
| FIR role | Nested interface generation (DI specific) | Hint function stub generation (generic) |
| Aggregation | Exclusion / replacement | None (validation only) |
| Core APIs | **Same** | **Same** |

### Constraints

- **Kotlin 2.3.x+ (K2 compiler) required** — does not work with K1
- The Compiler Plugin API is not binary compatible across minor versions; follow Kotlin upgrades
- `registerFunctionAsMetadataVisible()` / `referenceFunctions()` are not marked stable, but both metro and koin rely on them
