# kmp-error-handling Rule

[Japanese](./kmp-error-handling.ja.md)

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) rule for error handling and warning detection in Kotlin Multiplatform + Compose projects.

## Quick Start

### 1. Install the rule:

```bash
curl -fsSL \
  https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | \
  bash -s -- kmp-error-handling
```

### 2. Start coding:

When you modify error handling related files (`domain/**/error/`, `domain/**/util/`, `data/`, `core/**/*Catching*`, `app/**/ErrorHandling*`, `ui/**/error/`), Claude Code will automatically enforce the error handling conventions.

## What it does

This is a **path-triggered rule** that enforces consistent error handling patterns across a 4-layer KMP architecture:

### Key Conventions

1. **AppError sealed class** — Categorized error model with error codes (1xxx=Network, 2xxx=Auth, 3xxx=Data, 9xxx=Other) and `shouldAutoRetry` flag
2. **Error mapping** — Data layer converts raw exceptions (HTTP, IO, Serialization) to `AppError` via `toAppError()`
3. **CancellationException handling** — Always rethrown, never swallowed. Use `runSuspendCatching` instead of `runCatching` in suspend functions
4. **HandleError / HandleWarning** — Composable handler interfaces defined in Domain, implemented in UI/Data, wired in App via DI
5. **Retry/Recovery utilities** — Functional-style extension functions on `suspend () -> R` for retry, backoff, recover, timeout, measure

### Layer Responsibilities

| Layer | Responsibility |
|---|---|
| **Domain** | Define `AppError`, `HandleError`, `HandleWarning` interfaces; provide retry/recover utilities |
| **Data** | Map raw exceptions to `AppError`; implement `CrashlyticsHandleError` etc. |
| **UI** | Implement `HandleErrorDefault` with observable error list; use `handleError { }` in ViewModels |
| **App** | Wire DI bindings; compose multiple handlers with `plus` operator |

## Installed files

| File | Description |
|---|---|
| `.claude/rules/kmp-error-handling.md` | Rule definition (path-triggered) |

## Customization

After installation, adapt the error code categories and retry strategies to match your project's specific requirements.
