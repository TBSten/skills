# local-ticket-system

Markdown-based local ticket management system for any project.

## Install

```sh
gh skill install tbsten/skills local-ticket-system
```

## Overview

This skill sets up a `.local/ticket/` directory structure for managing tasks, bugs, and chapters as Markdown files. Tickets are tracked outside of Git (`.local/` is gitignored), making it easy to manage work-in-progress without polluting the repository history.

## Features

- **Ticket type selection** — Analyzes requirements to choose the right type (task / bug / chapter) before creating
- **Task tickets** (`task-{NNN}-{slug}.md`) — Track feature implementation with checklists
- **Bug tickets** (`bug-{NNN}-{slug}.md`) — Document bugs with reproduction steps and fix candidates
- **Chapter tickets** (`chapter-{slug}.md`) — Group related tasks/bugs under a higher-level goal, with scope, motivation, and a breakdown plan
- **Lifecycle management** — Tasks/bugs: active → `done/` → `closed/`. Chapters: active → split into tasks → `archived/`. Intentional deferral → `deferred/`
- **Emoji prefix titles** — Tickets carry a leading emoji prefix (e.g. `🗑️ Remove dead code`, `✨ Add login`, `🐛 Null pointer in user lookup`) for at-a-glance categorization
- **Template-based** — Consistent ticket format with built-in checklist items
- **Script-driven operations** — Bundled scripts handle the deterministic work (directory setup, sequence numbering, status moves); the AI focuses on judgment (ticket type, emoji, title, body)
- **Language/framework agnostic** — Works with any project type

## Directory Structure

```
.local/ticket/
├── about.md              # Operating rules
├── task-0xx-template.md  # Task ticket template
├── bug-0xx-template.md   # Bug ticket template
├── chapter-template.md   # Chapter template
├── task-xxx-*.md         # Active task tickets
├── bug-xxx-*.md          # Active bug tickets
├── chapter-*.md          # Active chapters
├── done/                 # Completed tickets (implemented & committed)
├── closed/               # Closed tickets (verified & validated)
├── archived/             # Archived chapters (all child tickets completed)
└── deferred/             # Deferred tickets (intentionally postponed)
```

## Scripts

The deterministic work is handled by bundled scripts (run them as-is; no need to read or reimplement them). On failure they print `ERROR:` / `WHY:` / `FIX:` to stderr.

| Script | Usage |
|--------|-------|
| `scripts/setup.sh` | Idempotent one-shot setup: creates `.local/ticket/` with the status subdirectories, copies the templates, and adds `.local/` to `.gitignore` if missing |
| `scripts/new-ticket.sh <task\|bug\|chapter> <slug>` | Computes the next sequence number (scanning `done/` `closed/` `deferred/` `archived/` too, per type) and creates the ticket from its template; prints the created path |
| `scripts/move-ticket.sh <file> <done\|closed\|deferred\|archived>` | Moves a ticket between statuses; for `deferred`, appends the reason block skeleton (with today's date) before moving |

## Ticket Types

| Type | Purpose | Granularity |
|------|---------|-------------|
| task | Single unit of work | Small–medium. Completable in one session |
| bug | Record and fix an existing defect | Small–medium. One ticket per bug |
| chapter | Group multiple tasks/bugs | Large. Requires planning, then split into tasks/bugs |

## Ticket Lifecycle

### task / bug

1. **Created** — Ticket placed in `.local/ticket/`
2. **In progress** — Work through the checklist items
3. **Done** — Implementation and commit complete → move to `done/`
4. **Closed** — Verification complete → move to `closed/`
5. **Deferred** — Intentionally postponed → move to `deferred/` (intended to revisit later)

### chapter

1. **Created** — Chapter placed in `.local/ticket/`
2. **Planning** — Refine scope and open questions
3. **Split** — Break down into task/bug tickets
4. **Archived** — All child tickets completed → move to `archived/`
5. **Deferred** — Postponed to a future phase → move to `deferred/`

### deferred/ rules

`scripts/move-ticket.sh <file> deferred` appends the following skeleton (with today's date) to the ticket before moving it; fill in the TODO placeholders afterwards:

```markdown
**Deferred 理由**: TODO:DeferredReason
**再起票 trigger**: TODO:ReopenTrigger
**Deferred 日付**: YYYY-MM-DD
```

To resume, move the ticket back from `deferred/` to `ticket/`.

## Prerequisites

- Git-managed project
- `.local/` must be in `.gitignore`
