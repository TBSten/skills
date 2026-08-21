# kotlin-compiler-plugin-dev

A skill for developing and reviewing Kotlin Compiler Plugins, and for adding/removing supported Kotlin versions in multi-version projects — backed by research data from 30+ existing plugins.

## What it does

This skill helps you:

1. **Find precedents** — Search 30+ existing compiler plugins to find implementations similar to what you want to build
2. **Choose Extension Points** — Determine the right FIR/IR Extension Points based on your requirements
3. **Review implementations** — Evaluate your compiler plugin against established patterns and best practices
4. **Review multi-version support** — Evaluate compat module layer / source set separation architecture; advise on tandem vs. independent release strategy
5. **Add/remove supported Kotlin versions** — Drive compat module additions (delegation pattern), capability flags, reflection shims, SSOT-driven CI matrix, kctfork version mapping, and Java 21 toolchain pinning
6. **Deep-dive into source code** — Use deepwiki MCP to read the latest source code of reference plugins

## When to use

- Starting a new compiler plugin project
- Adding a new feature to an existing compiler plugin
- Reviewing a compiler plugin implementation for correctness
- Researching how existing plugins implement a specific feature
- Deciding between FIR and IR for a particular transformation
- Adding or removing a supported Kotlin version on a project that already has multi-version infrastructure

## How it works

1. **Step 0**: Checks if deepwiki MCP is available (optional, enhances source code exploration)
2. **Step 1**: Understands your requirements (develop / review / research / add-or-remove supported Kotlin version)
3. **Step 2**: Searches `overview.md` — a table of 130+ Extension Point implementations across 30 plugins
4. **Step 3**: Reads `details/` files for in-depth implementation details (inheritance, overrides, behavior, diagnostics)
5. **Step 4**: Optionally uses deepwiki MCP to fetch the latest source code from GitHub
6. **Step 5**: Outputs a design proposal, review report, or research summary
7. **Step 6**: For add/remove supported Kotlin version mode, follows `references/multi-version-workflow.md` to add a compat module, update the SSOT / CI matrix, and verify per-version tests

## Bundled references

| File | Description |
|---|---|
| `references/overview.md` | Flat table of all Extension Points across 30+ plugins with source URLs |
| `references/patterns.md` | 4 architectural patterns + Extension Point selection guides + Multi-Version Support architectures (compat module layer / source set separation) |
| `references/review-checklist.md` | Checklists for K2 compatibility, design patterns, code quality, multi-Kotlin-version support |
| `references/details/*.md` | Source-code-level details for each plugin group (8 files) |
| `references/multi-version-workflow.md` | Detailed workflow for adding/removing supported Kotlin versions |
| `references/compat-module-setup.md` | `CompatContext` SPI / delegation pattern / ShadowJar packaging |
| `references/source-set-separation.md` | Source set separation approach in depth |
| `references/ci-matrix.md` | SSOT-driven dynamic matrix YAML template + per-version test script |
| `references/kotlin-tooling-version.md` | `KotlinToolingVersion` with Maturity-aware comparison |
| `references/version-gating.md` | Capability flag design + test-side self-skip |
| `references/reflection-shim.md` | Reflection shim to absorb small API drift without a new compat module |
| `references/troubleshooting.md` | Failure pattern → root cause → remediation table |

## Bundled assets

Ready-to-copy files for target projects that lack the multi-version test infrastructure (the skill copies them as-is instead of hand-writing equivalents):

| File | Copy to | Description |
|---|---|---|
| `assets/scripts/compiler-plugin-test.sh` | `scripts/` (+ `chmod +x`) | Per-version test runner; `--all` loops over the SSOT and reports failing versions |
| `assets/scripts/supported-kotlin-versions.txt` | `scripts/` | SSOT template for supported Kotlin versions (edit to match the project) |
| `assets/workflows/compiler-plugin-test.yml` | `.github/workflows/` | SSOT-driven dynamic CI matrix (resolve + test jobs) |

## Prerequisites

- A Kotlin project with compiler plugin source code (or a plan to create one)
- For supported-version add/remove, the project must already have multi-version infrastructure (compat module layer or source set separation). For initial setup, see the `kotlin-compiler-plugin-setup` skill (Step 4: Multi-Kotlin Version Support)
- Optional: deepwiki MCP server configured for enhanced source code exploration
