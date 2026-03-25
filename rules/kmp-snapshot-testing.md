# kmp-snapshot-testing Rule

[日本語](./kmp-snapshot-testing.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) rule for snapshot property-based testing (PBT) in Kotlin Multiplatform projects using Kotest and Turbine.

## Quick Start

### 1. Install the rule:

```bash
curl -fsSL \
  https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | \
  bash -s -- kmp-snapshot-testing
```

### 2. Start coding:

When you modify snapshot test code or test infrastructure, Claude Code will automatically read the testing documentation before making changes.

## What it does

This is a **path-triggered rule**. When snapshot test code or test infrastructure is modified, Claude Code reads the testing documentation first.

| Path pattern | Description |
|---|---|
| `**/jvmSnapshotTest/**/*.kt` | Snapshot test files |
| `core/testing/**/*.kt` | Core test infrastructure |
| `ui/core/testing/**/*.kt` | UI test infrastructure |

## Installed files

| File | Description |
|---|---|
| `.claude/rules/kmp-snapshot-testing.md` | Rule definition (path-triggered) |
| `docs/test/README.md` | Testing strategy overview |
| `docs/test/snapshot-test.md` | Snapshot PBT testing guide (state holders, logic) |
| `docs/test/compose-snapshot-test.md` | Compose UI snapshot testing guide |

## Related

If your project doesn't have snapshot testing infrastructure set up yet, use the `kmp-snapshot-testing-setup` skill:

```bash
npx skills add tbsten/skills \
  --skill kmp-snapshot-testing-setup
```
