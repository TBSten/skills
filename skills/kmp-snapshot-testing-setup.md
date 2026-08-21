# KMP Snapshot Testing Setup Skill

[日本語](./kmp-snapshot-testing-setup.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) skill that sets up snapshot testing infrastructure for Kotlin Multiplatform + Compose projects.

## Quick Start

### 1. Install the skill:

```bash
gh skill install tbsten/skills kmp-snapshot-testing-setup
```

### 2. Ask your AI agent:

```
Set up snapshot testing infrastructure for this project.
```

## How It Installs

File placement and text substitution are fully scripted: the agent runs `scripts/install.sh`
(`--project` / `--package` required; `--module-path`, `--ui-module-path`, `--skip-compose`,
`--dry-run`, `--force` optional). The script is idempotent — re-running never duplicates
catalog entries or `settings.gradle.kts` includes, and it never overwrites existing files
without `--force`. It prints a one-line result JSON (conflicts / warnings / manual follow-ups).
The agent then handles project-specific follow-ups (serialization plugin classpath in
build-logic, adapting `AppTheme` / `WithTestGraph`, catalog alias alignment) and verifies
the build.

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
| `core/testing/snapshot` | build.gradle.kts (template), ProjectConfig, shouldMatchSnapshot, StateHolderSnapshotPbtSpec, LogicSnapshotPbtSpec, KotlinCodeFormat, PBT utilities |
| `ui/core/testing` | build.gradle.kts (template), ComposeSnapshotPbtSpec, runComposableSnapshotTest |

Both modules are wired into `settings.gradle.kts` (includes) and the version catalog
(`gradle/libs.versions.toml`: kotest / turbine / kotlinx-serialization / coroutines-test entries)
by the install script.

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
