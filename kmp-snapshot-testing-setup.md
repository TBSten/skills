# KMP Snapshot Testing Setup Skill

[日本語](./kmp-snapshot-testing-setup.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that sets up snapshot testing infrastructure for Kotlin Multiplatform + Compose projects.

## Quick Start

### 1. Install the skill:

```bash
npx skills add tbsten/skills --skill kmp-snapshot-testing-setup
```

### 2. Ask your AI agent:

```
Set up snapshot testing infrastructure for this project.
```

## What Gets Set Up

### Build Logic (Convention Plugins)

| File | Description |
|---|---|
| `convention-kmp-test.gradle.kts` | Adds Kotest, Turbine, coroutines-test to commonTest/jvmTest |
| `convention-kmp-snapshot-testing.gradle.kts` | Registers jvmSnapshotTest compilation, Record/Verify/Report tasks |
| `SnapshotReportTask.kt` | Generates diff reports (JSON + Markdown + HTML) |

### Testing Modules

| Module | Components |
|---|---|
| `core/testing/snapshot` | ProjectConfig, shouldMatchSnapshot, StateHolderSnapshotPbtSpec, LogicSnapshotPbtSpec, KotlinCodeFormat, PBT utilities |
| `ui/core/testing` | ComposeSnapshotPbtSpec, runComposableSnapshotTest |

### Shell Scripts

| Script | Description |
|---|---|
| `tools/snapshot-diff.sh` | Orchestrator: worktree-based record → verify → report |
| `tools/snapshot-diff/step1-5` | Individual steps for the diff workflow |

## Usage After Setup

```bash
# Run snapshot diff against main branch
./tools/snapshot-diff.sh -before=main

# Fast check with fewer PBT iterations
./tools/snapshot-diff.sh -before=main -pbt-iteration=10

# Check build/snapshots/result.html for visual report
```

## Repository

This skill is part of [TBSten/skills](https://github.com/TBSten/skills).
