# feedback-mode

Build a **debug-only feedback mode** into your app, and let Claude Code triage the feedback it collects.

> The skill body is written in Japanese. This page is an English overview.

## Install

```sh
gh skill install tbsten/skills feedback-mode
```

## Overview

In the feedback mode, a tester (or you) selects a UI part in the running app and writes a comment.
The app attaches context automatically (which part, the screen state, recent logs, a screenshot with the
selected part outlined) and stores **one record per feedback** in a feedback inbox. Claude Code then reads the inbox,
splits the records into implement / investigate / design work for subagents, archives the handled ones, and reports.

The skill is a **branching router**. On start it inspects the environment (`scripts/detect.sh`):

| Inbox | Wiring | Verdict | What is read |
|---|---|---|---|
| exists | exists / missing | `loop` | only `references/loop/` (triage loop) |
| missing | exists (partial) | `partial` | a few steps of the build guide, then the loop |
| missing | missing | `build` | only `references/build/` (construction) |

Only one side is read, so unrelated instructions never enter the context.

## When to use

- You develop a desktop (Electron / Tauri), web, or mobile (iOS / Android / React Native / Flutter) app and want
  in-app feedback that carries screen, state, logs, and screenshots into a Claude Code workflow
- You want to change where feedback is stored (local files, an HTTP receiver on a dev machine, an issue tracker, cloud storage)
- Feedback has piled up in a folder and you want Claude Code to work through it with subagents
- You already have a debug menu / dev panel and want to hang the feedback entry off it

## Design policies

- **Sink abstraction**: the inbox is a `Sink` interface (`putAttachments` / `putEntry` / `list` / `read` / `archive`).
  Implementations shipped: local files, memory (tests), HTTP. Issue tracker and cloud storage are outlined only
- **Debug builds only**: wiring is removed at build time (DCE + dynamic import, `#if DEBUG`, debug source set,
  separate entrypoint, ...), with a runtime `enabled` guard as insurance, and a script greps the production artifact
- **Reuse the existing debug infrastructure**: if the app already has a debug menu / dev panel, add one entry there
  instead of creating a new entry point or guard
- **Logging is a separate task**: the feedback mode only reads logs through a `getRecentLogs` hook and attaches them

## Contents

| Path | Purpose |
|---|---|
| `SKILL.md` | Router: detect the environment, read only the build or loop side |
| `references/concepts.md` | Terms, record format, state transitions, the Sink contract (shared) |
| `references/build/` | Build guide (steps, checklist), architecture, per-platform guidance |
| `references/loop/` | Triage loop (rules, classification, history, report), subagent prompts, full check, lessons learned |
| `assets/templates/` | Code to copy into your app: core (Sinks, numbering, redact, Markdown builder, node:test tests), a TypeScript core with a vitest Sink contract (`core-ts/`), Electron main wiring, web UI, an Astro Dev Toolbar integration (`web/astro/`), mobile (React Native / Flutter), existing-debug-menu examples, JSON schema, record and memo examples |
| `scripts/detect.sh` | Read-only environment check that prints `VERDICT=loop` / `partial` / `build` |
| `scripts/copy-core.sh` | Copies the core templates into your app and runs the `node:test` suite |
| `scripts/check-no-feedback-in-prod.sh` | Greps a production build artifact for leftovers of the feedback mode |

## Maturity and known limitations

Status: **Experimental**. The reference implementation is an internal Electron tool that has run this loop for weeks.
A second reference implementation (a web docs site built with Astro 7 + Starlight) added the Astro Dev Toolbar integration, the schema 1 record, location hints, the memo feature and the TypeScript core.
Verified here: the core templates (36 `node:test` tests across memory / file / HTTP Sinks), the TypeScript core (32 vitest tests), the Astro integration in a minimal Astro 7 project, the web UI with the dev server in headless Electron,
and the production-artifact check against fake artifacts. **Not verified**: the Electron main wiring, Tauri, React Native, Flutter,
native iOS / Android snippets, html2canvas screenshots, HTTP over a LAN / on real devices, the per-platform "strip from production" build settings,
and Issue-tracker / cloud-storage Sinks. Always inspect your release artifact before shipping.
