# Add/Remove Supported Kotlin Version Skill

[日本語](./add-support-kotlin-version.ja.md)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that adds or removes a specific Kotlin version from a Kotlin Compiler Plugin project's support matrix.

## Quick Start

### 1. Install the skill:

```bash
npx skills add tbsten/skills \
  --skill add-support-kotlin-version
```

### 2. Ask your AI agent:

```
Add Kotlin 2.4.0 support to this compiler plugin project.
```

## What This Skill Does

For compiler plugin projects that already have multi-version support infrastructure, this skill:

1. Determines whether the target Kotlin version fits within an existing compat module's range (or needs a new module)
2. Updates CI matrix, kctfork version map, version catalog, and README
3. Creates a new compat module if needed (copying existing, renaming packages, updating `minVersion`)
4. Runs tests across all supported versions to verify

## Supported Architectures

### A: Compat Module Layer (metro-style)

Projects with `compiler-plugin/compat-kXX/` modules. ServiceLoader dispatches to the best-matching implementation at runtime.

Each compat module has a `minVersion`. The dispatch algorithm selects the factory with the highest `minVersion` that is ≤ the current compiler version.

### B: Source Set Separation

Projects with `src/v2_0_0/kotlin/` or `src/pre_2_0_0/kotlin/` directories. Gradle dynamically includes the appropriate source directory based on the Kotlin version at build time.

Best for absorbing K1 (PSI/ComponentRegistrar) vs K2 (FIR/CompilerPluginRegistrar) divergence.

## Key Concepts

### compat module minVersion range

```
compat-k2000: minVersion="2.0.0"  → covers 2.0.0–2.0.1x
compat-k2020: minVersion="2.0.20" → covers 2.0.20–2.1.x
compat-k23:   minVersion="2.2.0"  → covers 2.2.0–(latest)
```

A new compat module is needed only when:
- The target version falls outside all existing ranges AND
- `NoSuchMethodError`/`NoClassDefFoundError` occurs (API boundary)

### kctfork version mapping

Unit tests use [kctfork (kotlin-compile-testing)](https://github.com/ZacSweers/kotlin-compile-testing). Each Kotlin version requires a compatible kctfork version. The skill maintains the mapping and updates it when adding a new major/minor.

### fail-fast: false in CI

Setting `fail-fast: false` in the GitHub Actions matrix is critical — it ensures stable-version test results are visible even when experimental/RC versions fail.

## Prerequisites

This skill assumes the project already has multi-version support infrastructure. For initial setup, use the `kotlin-compiler-plugin-setup` skill (Step 10: Multi-Kotlin Version Support).

## References

- kctfork releases: https://github.com/ZacSweers/kotlin-compile-testing/releases
- Compose Multiplatform releases: https://github.com/JetBrains/compose-multiplatform/releases
- Kotlin releases: https://github.com/JetBrains/kotlin/releases
- Real-world examples: [ZacSweers/metro](https://github.com/ZacSweers/metro), [kitakkun/multi-kotlin-support-example](https://github.com/kitakkun/multi-kotlin-support-example)
