---
status: Active
group: Kotlin / Android アプリ開発
---

# kmp-snapshot-testing Rule

[日本語](./kmp-snapshot-testing.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) rule for snapshot property-based testing (PBT) in Kotlin Multiplatform projects using Kotest and Turbine.

## Quick Start

### 1. Install the rule:

```bash
curl -fsSL https://rules.tbsten.me/i | \
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

### Test skeleton templates

New tests start from the skeletons in `docs/test/templates/` (copy & fill the TODOs) instead of having the AI write the boilerplate from scratch:

| Template | For |
|---|---|
| `templates/__Target__PbtSnapshotTest.kt` | StateHolder / ViewModel PBT snapshot tests |
| `templates/__Target__LogicPbtSnapshotTest.kt` | Logic / function PBT snapshot tests |
| `templates/__Target__ComposeSnapshotPbt.kt` | Compose UI PBT snapshot tests |

## Installed files

| File | Description |
|---|---|
| `.claude/rules/kmp-snapshot-testing.md` | Rule definition (path-triggered) |
| `docs/test/README.md` | Testing strategy overview |
| `docs/test/snapshot-test.md` | Snapshot PBT testing guide (state holders, logic) |
| `docs/test/compose-snapshot-test.md` | Compose UI snapshot testing guide |
| `docs/test/templates/*.kt` | Test skeleton templates (copy & fill the TODOs) |

> **Note:** Re-running the installer overwrites the installed files, including any local customizations you made under `docs/test/`.

## Related

If your project doesn't have snapshot testing infrastructure set up yet, use the `kmp-snapshot-testing-setup` skill:

```bash
gh skill install tbsten/skills kmp-snapshot-testing-setup
```
