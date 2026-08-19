# KSP Plugin Setup Skill

[日本語](./ksp-plugin-setup.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that scaffolds a KSP plugin
(Symbol Processor) project in the layout [cream.kt](https://github.com/TBSten/cream) arrived at —
three modules, a four-layer processor, a golden-file test harness, and architecture rules that keep
the shape from drifting afterwards.

A one-shot version that needs no install is available as the
[`ksp-plugin-setup` prompt](../prompts/ksp-plugin-setup.md).

## Quick Start

### 1. Install the skill:

```bash
gh skill install tbsten/skills ksp-plugin-setup
```

### 2. Ask your AI agent:

```
Set up a KSP plugin project.
```

## What Gets Set Up

### Module Structure

| Module | Description |
|---|---|
| `<project-name>-runtime/` | Annotation declarations **only** — zero runtime logic, so it targets every Kotlin platform. Published |
| `<project-name>-ksp/` | The processor. JVM only (a KSP limitation), `-Xcontext-parameters`. Published |
| `test/` | KMP integration tests: applies the processor for real and verifies generated code behaviour on every target |
| `buildLogic/` | Included build sharing the root version catalog; one minimal convention plugin (lint) |

### Processor Layers

| Layer | Responsibility |
|---|---|
| root (`ksp/*.kt`) | Exactly three files: `SymbolProcessor`, `Provider`, `ProcessContext`. Orchestration only |
| `feature/<name>/` | One annotation, one directory, one entry point: discover → validate → call core |
| `core/<sub>/` | Project-specific generation, plus a leaf `core/error` exception hierarchy |
| `options/` | The KSP option model and its parsing, in one data class |
| `util/` · `util/ksp/` | Generic helpers only; the top level stays free of the KSP API |

Dependencies run one way — `feature → core → util` — with `feature → ProcessContext` as the single
upward edge. Feature-to-feature dependencies are forbidden.

### Testing Infrastructure

| Component | Description |
|---|---|
| kctfork | Real in-process Kotlin + KSP compilation for end-to-end tests |
| Facet golden files | One Markdown file per scenario holding input, options, exit code, console output and generated sources |
| Generator-driven snapshots | KotlinPoet-built scenario families crossed with an option matrix; each point is one test and one golden |
| Diagnostic goldens | The error message text itself is pinned, alongside the exit code |
| Konsist | Import-based enforcement of the layering, the root allow-list, and a per-file line budget |

### Build & CI

| File | Description |
|---|---|
| `gradle/libs.versions.toml` | Single source of truth, including the project's own version |
| `gradle.properties` | Configuration cache + build cache, `ksp.incremental=false` |
| `test/build.gradle.kts` | The KSP × KMP workaround (and why the `*Test` KSP tasks must stay enabled) |
| `.github/workflows/gradle.yml` | `matrix.include` to minimise runner cost, with concurrency and timeouts |
| `.github/workflows/publish.yml` | Triggered by GitHub Release `published` (fires for pre-releases too) |

### Rules Installed Into Your Project

Five `.claude/rules/*.md` files are placed in the generated project, path-scoped so they load only
when the relevant files are being edited: the architecture table, the root/feature/core placement
rules, and the test conventions. They are paired with the Konsist specs — change the table, change
the specs.

## Key Concepts

### Diagnostics are reported, never thrown

User misuse becomes `logger.error(message, ksNode)` followed immediately by a `return`. Throwing
turns a clean COMPILATION_ERROR into an opaque KSP INTERNAL_ERROR and can leave a half-written file
behind. Every diagnostic carries a solution, not only a description of the problem.

### Per-annotation differences without `when`

`GenerateSourceAnnotation` is deliberately not `sealed`. Adding an annotation means adding one
implementation file and overriding only the rules that differ — no existing branch is edited.
Per-property rules are standalone function types passed to generators as ordinary parameters.

### Transactional generation

Every file goes through one write-out helper that owns the `package` and import boilerplate and
buffers the body first. If nothing was written, no file is opened — an empty `package` + `import`
file is worse than no file.

### Options parse lazily

Parsing in the processor's constructor turns a bad option value into an INTERNAL_ERROR with no useful
message. Parsing inside `process()` makes it a reported COMPILATION_ERROR instead.

## Related Skills

- [`kotlin-maven-central-publish`](./kotlin-maven-central-publish.md) — the full Maven Central setup
  (GPG, secrets, Sonatype) this skill deliberately does not duplicate
- [`kotlin-compiler-plugin-setup`](./kotlin-compiler-plugin-setup.md) — for a Kotlin **compiler**
  plugin (FIR/IR) rather than a symbol processor
