# kmp-layered-architecture Rule

[日本語](./kmp-layered-architecture.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) rule that enforces a 4-layer architecture (App / UI / Domain / Data) for Kotlin Multiplatform + Compose projects.

## Quick Start

### 1. Install the rule:

```bash
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- kmp-layered-architecture
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

## Installed files

| File | Description |
|---|---|
| `.claude/rules/kmp-layered-architecture.md` | Rule definition (path-triggered) |
| `docs/architecture/README.md` | Architecture overview and layer dependency diagram |
| `docs/architecture/app.md` | App layer: entry points, DI, flavor processing |
| `docs/architecture/ui.md` | UI layer: screens, ViewModels, navigation |
| `docs/architecture/domain.md` | Domain layer: UseCases, Repositories (interfaces) |
| `docs/architecture/data.md` | Data layer: Repository implementations, API clients |

## Customization

After installation, edit the documents in `docs/architecture/` to match your project's specific architecture decisions, naming conventions, and DI framework.
