# Collector Compiler Plugin Skill

[日本語](./collector-compiler-plugin.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that scaffolds a Kotlin K2 Compiler Plugin which collects annotated classes across Gradle modules at compile time — a "compile-time ServiceLoader" built on the **hint function pattern**.

> This skill was moved from [TBSten/collector-compiler-plugin-skill](https://github.com/TBSten/collector-compiler-plugin-skill) and is now maintained in this repository.

## Quick Start

### 1. Install the skill:

```bash
gh skill install tbsten/skills collector-compiler-plugin
```

### 2. Ask your AI agent:

```
I want to collect classes annotated with @Collectable across modules at compile time.
```

## Background

[metro](https://github.com/ZacSweers/metro) and [koin-compiler-plugin](https://github.com/InsertKoinIO/koin-compiler-plugin) both use a Kotlin Compiler Plugin to discover and aggregate annotated classes across multiple modules at compile time. The mechanism is useful beyond DI frameworks, but implementing a compiler plugin has a high barrier to entry. This skill generalizes the pattern so you can add the same mechanism to your own project. It covers cross-module collection that KSP cannot do.

## How It Works

1. **Hearing** — project type (KMP / Android / JVM), what to collect, tag grouping, base package, annotation names
2. **Design review** — annotation names/arguments, hint package (`<base-package>.hints`), module layout, generated file list
3. **Code generation** — reads `references/architecture.md` and customizes `references/templates/*` for your package and annotation names
4. **Verification guide** — `./gradlew :compiler-plugin:test`, `publishToMavenLocal`, then try it in a sample project

### Generated Modules

| Module | Description |
|---|---|
| `annotations/` | `@Collectable` / `@CollectAll` marker annotations (KMP, runtime dependency) |
| `compiler-plugin/` | `CompilerPluginRegistrar` + FIR / IR extensions (JVM, compile-time only) |
| `gradle-plugin/` | Gradle plugin that applies the compiler plugin |

Tests use `kotlin-compile-testing`.

### User-facing API

```kotlin
@Collectable(tag = "api-handler")
class UserApiHandler : ApiHandler { ... }

// The compiler generates the body
@CollectAll(tag = "api-handler")
fun collectApiHandlers(): List<KClass<*>>
```

## Key Concepts

### Hint Function Pattern

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

### metro vs koin

| Aspect | metro | koin |
|---|---|---|
| Hint package | `metro.hints` | `org.koin.plugin.hints` |
| Function name | Derived from scope name (`appScope`) | prefix + metadata (`componentscan_*_single`) |
| Metadata storage | Parameter types only | Function name + parameter names/types |
| FIR role | Nested interface generation (DI specific) | Hint function stub generation (generic) |
| Aggregation | Exclusion / replacement | None (validation only) |
| Core APIs | **Same** | **Same** |

## Constraints

- **Kotlin 2.3.x+ (K2 compiler) required** — does not work with K1
- The Compiler Plugin API is not binary compatible across minor versions; follow Kotlin upgrades
- `registerFunctionAsMetadataVisible()` / `referenceFunctions()` are not marked stable, but both metro and koin rely on them
