# Kotlin Library Bootup Skill

[日本語](./kotlin-library-bootup.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that boots up a plain Kotlin /
Kotlin Multiplatform library project in the shape several production OSS libraries arrived at
(koma-strict, katachi, compose-preview-lab, cream and others) — convention plugins, binary
compatibility checks, Maven Central publishing, an integration-test build, a docs site and CI —
and verifies that it builds, tests and publishes locally.

The mechanical work — copying the templates, choosing target-specific blocks, substituting every
placeholder, renaming files and checking that nothing was left unreplaced — is done
deterministically by `scripts/scaffold.sh`. `scripts/verify.sh` bootstraps the Gradle wrapper and
runs the build checks with logs saved under `.local/tmp/`. The agent only confirms the inputs, runs
the scripts and reviews the result.

## Quick Start

### 1. Install the skill:

```bash
gh skill install tbsten/skills kotlin-library-bootup
```

### 2. Ask your AI agent:

```
Create a new Kotlin Multiplatform library.
```

## Targets

| `--targets` | Targets |
|---|---|
| `standard` (default) | android / jvm / js / wasmJs / iosArm64 / iosSimulatorArm64 |
| `full` | standard + macOS / watchOS / tvOS / Linux / Windows / androidNative / wasmWasi |
| `jvm` | `kotlin("jvm")` only. No Android / Apple configuration or CI jobs are generated |

Apple targets are enabled on macOS only when Xcode is installed, so a machine with just the
Command Line Tools still builds. `-PenableAppleTargets=true` / `-PdisableAppleTargets=true` override it.

## What Gets Set Up

### Project Structure

| Path | Description |
|---|---|
| `<name>-core/` | The first library module: `explicitApi()`, `@Internal<Name>Api` / `@Experimental<Name>Api` opt-in markers, a sample API and a kotest spec |
| `build-logic/` | Included build with convention plugins `<name>.kmp` / `.jvm` / `.publish` / `.lint` / `.test`, sharing the root version catalog |
| `integrationTest/` | A standalone Gradle build that depends on the library through its Maven coordinates, substituted by `includeBuild("../")` (optional) |
| `docs/` | Astro Starlight site (en / ja) with the Dokka API reference under `/api-docs/` (optional) |
| `.github/workflows/` | `ci.yml`, `publish.yml`, `docs.yml`, `clear-ci-cache.yml` (optional) |
| `README.md` / `README.ja.md` / `CLAUDE.md` / `.claude/rules/` | Project docs and conventions for humans and agents |
| `.claude/skills/` | `bump-library-version` / `release-note` / `verify-changes` (optional) |

### Build & Quality

| Component | Description |
|---|---|
| Version catalog | The single source of truth, including the library's own version and build constants |
| kotest | FreeSpec specs in `commonTest`, run on every target (KSP + kotest plugin for JS / Wasm / Native, Android host tests enabled) |
| binary-compatibility-validator | `apiDump` / `apiCheck`, with klib validation for KMP |
| ktlint | Rules live in `.editorconfig` |
| Backward-compatibility floor | Published metadata readable by Kotlin 2.2 on JVM / Android |
| vanniktech maven-publish | Coordinates derived from the project path, Dokka javadoc jar, signing skipped for `publishToMavenLocal` |

### CI & Release

| Workflow | Description |
|---|---|
| `ci.yml` | lint / api check / per-platform tests / publish dry run / integration test / docs build, aggregated by a `final` job |
| `publish.yml` | On GitHub Release `published`: checks that the tag matches the catalog version and is not a SNAPSHOT, runs CI, then publishes to Maven Central |
| `docs.yml` | Builds Dokka + Starlight and deploys to GitHub Pages |

## Key Concepts

### Delegation, not duplication

This skill only builds the foundation of a plain library. KSP processors, compiler plugins,
snapshot testing, Maven Central secrets and IntelliJ plugins are handled by the dedicated skills
listed below, which can be used on top of a project generated here.

### Every choice has a source

Each technique in the generated project is taken from a library that runs it in production, and
`references/techniques.md` records where it came from and why it is there.

### The Kotlin floor on KMP

Lowering `coreLibrariesVersion` breaks JS / Wasm compilation, because their stdlib klib must match
the compiler's ABI. The build resolves the stdlib at the compiler version for JS / Wasm only, while
the published metadata keeps the floor. JVM / Android consumers need Kotlin 2.2+, and JS / Wasm /
Native consumers need the Kotlin version the library was built with.

## Related Skills

- [`ksp-plugin-setup`](./ksp-plugin-setup.md) — for a KSP symbol processor
- [`kotlin-compiler-plugin-setup`](./kotlin-compiler-plugin-setup.md) / [`kotlin-compiler-plugin-dev`](./kotlin-compiler-plugin-dev.md) — for a Kotlin compiler plugin
- [`kmp-snapshot-testing-setup`](./kmp-snapshot-testing-setup.md) — snapshot and property-based testing
- [`kotlin-maven-central-publish`](./kotlin-maven-central-publish.md) — GPG keys and GitHub Secrets for publishing
- [`intellij-plugin-dev`](./intellij-plugin-dev.md) — for an IntelliJ plugin
