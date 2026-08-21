---
status: Active
group: Kotlin / Android アプリ開発
---

# kmp-layered-architecture Rule

[日本語](./kmp-layered-architecture.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) rule that enforces a 4-layer architecture (App / UI / Domain / Data) for Kotlin Multiplatform + Compose projects.

## Quick Start

### 1. Install the rule:

```bash
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
```

### 2. Start coding:

When you modify files in `app/`, `ui/`, `domain/`, or `data/` directories, Claude Code will automatically read the architecture documentation before making changes.

## What it does

This is a **path-triggered rule**. When code in any of the 4 layers is modified, Claude Code is instructed to read the corresponding architecture document first.

| Path pattern | Document |
|---|---|
| `app/**/*.kt` | `docs/architecture/app.md` |
| `ui/**/*.kt` | `docs/architecture/ui.md` |
| `domain/**/*.kt` | `docs/architecture/domain.md` |
| `data/**/*.kt` | `docs/architecture/data.md` |

### Mechanical convention checks & feature scaffolding

Instead of relying on the AI to remember the conventions, the rule ships:

- **`docs/architecture/templates/ArchitectureConventionTest.kt`** — a [Konsist](https://docs.konsist.lemonappdev.com/) test template that verifies layer dependencies (per the diagram in `README.md`), the `Providers` suffix rule (no `Module` / singular `Provider`), and the `Impl` / `Fake` naming rules. Copy it into a jvmTest source set and replace the TODO constants at the top.
- **`tools/kmp-layered-architecture/new-feature.sh`** — a scaffold script that generates `ui/feature/<name>/` (Screen / ViewModel / Navigation) from `docs/architecture/templates/feature/`. Additions to existing files (settings.gradle.kts, ui/navigation files, DI Providers) cannot be automated, so the script prints the snippets to stdout for you (or the AI) to apply:

```bash
bash tools/kmp-layered-architecture/new-feature.sh Home com.example.app.ui.feature.home
```

## Installed files

| File | Description |
|---|---|
| `.claude/rules/kmp-layered-architecture.md` | Rule definition (path-triggered) |
| `docs/architecture/README.md` | Architecture overview and layer dependency diagram |
| `docs/architecture/app.md` | App layer: entry points, DI, flavor processing |
| `docs/architecture/ui.md` | UI layer: screens, ViewModels, navigation |
| `docs/architecture/domain.md` | Domain layer: UseCases, Repositories (interfaces) |
| `docs/architecture/data.md` | Data layer: Repository implementations, API clients |
| `docs/architecture/templates/ArchitectureConventionTest.kt` | Konsist convention test template |
| `docs/architecture/templates/feature/*` | Feature scaffold templates (Screen / ViewModel / Navigation) |
| `tools/kmp-layered-architecture/new-feature.sh` | Feature scaffold script (run with `bash`) |

## Customization

After installation, edit the documents in `docs/architecture/` to match your project's specific architecture decisions, naming conventions, and DI framework.

> **Note:** Re-running the installer overwrites the installed files, including any local customizations you made under `docs/architecture/` and `tools/kmp-layered-architecture/`.
