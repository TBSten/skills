# local-ticket-system

Markdown-based local ticket management system for any project.

## Overview

This skill sets up a `.local/ticket/` directory structure for managing tasks and bugs as Markdown files. Tickets are tracked outside of Git (`.local/` is gitignored), making it easy to manage work-in-progress without polluting the repository history.

## Features

- **Task tickets** (`task-{NNN}-{slug}.md`) — Track feature implementation with checklists
- **Bug tickets** (`bug-{NNN}-{slug}.md`) — Document bugs with reproduction steps and fix candidates
- **Lifecycle management** — Move tickets through stages: active → `done/` → `closed/`
- **Template-based** — Consistent ticket format with built-in checklist items
- **Language/framework agnostic** — Works with any project type

## Directory Structure

```
.local/ticket/
├── about.md              # Operating rules
├── task-0xx-template.md  # Ticket template
├── task-xxx-*.md         # Active task tickets
├── bug-xxx-*.md          # Active bug tickets
├── done/                 # Completed tickets (implemented & committed)
└── closed/               # Closed tickets (verified & validated)
```

## Ticket Lifecycle

1. **Created** — Ticket placed in `.local/ticket/`
2. **In progress** — Work through the checklist items
3. **Done** — Implementation and commit complete → move to `done/`
4. **Closed** — Verification complete → move to `closed/`

## Prerequisites

- Git-managed project
- `.local/` must be in `.gitignore`

## Install

```sh
npx skills add tbsten/skills --skill local-ticket-system
```
