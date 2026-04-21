# kotlin-compiler-plugin-dev

A skill for developing and reviewing Kotlin Compiler Plugins, backed by research data from 30+ existing plugins.

## What it does

This skill helps you:

1. **Find precedents** — Search 30+ existing compiler plugins to find implementations similar to what you want to build
2. **Choose Extension Points** — Determine the right FIR/IR Extension Points based on your requirements
3. **Review implementations** — Evaluate your compiler plugin against established patterns and best practices
4. **Review multi-version support** — Evaluate compat module layer / source set separation architecture; advise on tandem vs. independent release strategy
5. **Deep-dive into source code** — Use deepwiki MCP to read the latest source code of reference plugins

## When to use

- Starting a new compiler plugin project
- Adding a new feature to an existing compiler plugin
- Reviewing a compiler plugin implementation for correctness
- Researching how existing plugins implement a specific feature
- Deciding between FIR and IR for a particular transformation

## How it works

1. **Step 0**: Checks if deepwiki MCP is available (optional, enhances source code exploration)
2. **Step 1**: Understands your requirements (develop / review / research)
3. **Step 2**: Searches `overview.md` — a table of 130+ Extension Point implementations across 30 plugins
4. **Step 3**: Reads `details/` files for in-depth implementation details (inheritance, overrides, behavior, diagnostics)
5. **Step 4**: Optionally uses deepwiki MCP to fetch the latest source code from GitHub
6. **Step 5**: Outputs a design proposal, review report, or research summary

## Bundled references

| File | Description |
|---|---|
| `references/overview.md` | Flat table of all Extension Points across 30+ plugins with source URLs |
| `references/patterns.md` | 4 architectural patterns + Extension Point selection guides + Multi-Version Support architectures (compat module layer / source set separation) |
| `references/review-checklist.md` | Checklists for K2 compatibility, design patterns, code quality, multi-Kotlin-version support |
| `references/details/*.md` | Source-code-level details for each plugin group (8 files) |

## Prerequisites

- A Kotlin project with compiler plugin source code (or a plan to create one)
- Optional: deepwiki MCP server configured for enhanced source code exploration
