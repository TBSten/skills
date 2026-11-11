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
| missing (only `review/`) | missing | `review` | only `references/review/` (the side where a human answers the agent's findings) |
| missing | missing | `build` | only `references/build/` (construction) |

Only one side is read, so unrelated instructions never enter the context.

It also covers the other direction (agent → human):

| Direction | Tool | What it does |
|---|---|---|
| agent → human | **Review** | The agent writes review / check findings to `<Inbox>/review/*.md`; a human reads them as marks and popups on the page (or in chat) and answers with buttons: resolved / won't fix / send as feedback / buttons the human added. Each finding carries `impact` (does it affect users) and `recommend` (a 0–10 score per button). CLI: `scripts/review.mjs` |
| human's answer → next run | **Decisions** | "Won't fix / as intended / later / policy" answers are kept in git (`.claude/feedback-decisions/`) and checked before the next run reports findings, so the same question is never asked twice. `scripts/decisions.py` |
| human → agent (an HTML the agent made) | **Static HTML feedback** | On a single-file report or checklist the agent generates, the human picks items and comments (Shift for several, quotes, drafts kept), then copies all comments together with answers and pastes them into the chat. Targets are paths into the JSON embedded in the HTML |

## When to use

- You develop a desktop (Electron / Tauri), web, or mobile (iOS / Android / React Native / Flutter) app and want
  in-app feedback that carries screen, state, logs, and screenshots into a Claude Code workflow
- You want to change where feedback is stored (local files, an HTTP receiver on a dev machine, an issue tracker, cloud storage)
- Feedback has piled up in a folder and you want Claude Code to work through it with subagents
- You already have a debug menu / dev panel and want to hang the feedback entry off it
- The agent produces many findings (reviews, pre-release checks) and you want a human to read them in place and answer with a button, not in a table
- You do not want to be asked again next run about a finding you already declined
- You want people to comment directly on an HTML report the agent generated

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
| `SKILL.md` | Router: detect the environment, read only one of build / loop / review |
| `references/concepts.md` | Terms, record format, state transitions, the Sink contract (shared) |
| `references/build/` | Build guide (steps, checklist), architecture, per-platform guidance, static HTML feedback (`static-html.md`) |
| `references/loop/` | Triage loop (rules, classification, history, report), subagent prompts, full check, lessons learned |
| `references/review/` | Review: file format, flow, buttons, added choices, CLI (`review.md`); rules for the marks-and-popup UI (`ui.md`); decisions (`decisions.md`) |
| `assets/templates/` | Code to copy into your app: core (Sinks, numbering, redact, Markdown builder, node:test tests), a TypeScript core with a vitest Sink contract (`core-ts/`), Electron main wiring, web UI, an Astro Dev Toolbar integration (`web/astro/`), mobile (React Native / Flutter), existing-debug-menu examples, JSON schema, record and memo examples, Review finding / choice / decision examples (`review/`) |
| `scripts/detect.sh` | Read-only environment check that prints `VERDICT=loop` / `partial` / `review` / `build` |
| `scripts/review.mjs` | Review CLI (list / show / check / resolve / remove / reopen / send / archived / actions). A UI's dev-server handler imports it to read and write with the same rules. Node 18+, no dependencies |
| `scripts/decisions.py` | Decisions: check, match against findings, copy from Review (record-review). Python 3 standard library only |
| `scripts/copy-core.sh` | Copies the core templates into your app and runs the `node:test` suite |
| `scripts/check-no-feedback-in-prod.sh` | Greps a production build artifact for leftovers of the feedback mode |

## Maturity and known limitations

Status: **Experimental**. The reference implementation is an internal Electron tool that has run this loop for weeks.
A second reference implementation (a web docs site built with Astro 7 + Starlight) added the Astro Dev Toolbar integration, the schema 1 record, location hints, the memo feature and the TypeScript core.
Verified here: the core templates (36 `node:test` tests across memory / file / HTTP Sinks), the TypeScript core (32 vitest tests), the Astro integration in a minimal Astro 7 project, the web UI with the dev server in headless Electron,
and the production-artifact check against fake artifacts. Later, Review (marks and popups for findings, human-added choices, user impact and recommendation scores), decisions and static HTML feedback, all run on the same web reference implementation, were generalized and added (19 `node:test` tests for `review.mjs`, 21 unittest tests for `decisions.py`; the UI is specified but ships no template code). **Not verified**: the Electron main wiring, Tauri, React Native, Flutter,
native iOS / Android snippets, html2canvas screenshots (the web reference implementation's `modern-screenshot` is verified), HTTP over a LAN / on real devices, the per-platform "strip from production" build settings,
and Issue-tracker / cloud-storage Sinks. Always inspect your release artifact before shipping.
