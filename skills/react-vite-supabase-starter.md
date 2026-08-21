# react-vite-supabase-starter

Scaffold a new web application with React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase.

## Install

```sh
gh skill install tbsten/skills react-vite-supabase-starter
```

## Overview

This skill provides a complete project scaffold for building modern SPAs with:

- **React 19** with TypeScript (strict mode)
- **Vite** as build tool with `@` path aliases
- **Tailwind CSS v4** with CSS variables theming
- **shadcn/ui** (Radix UI) for accessible components
- **TanStack Router** for type-safe routing with auth guards
- **TanStack Query** for server state management
- **Supabase** for auth and database (optional)
- **Vitest** + Testing Library for unit tests
- **Playwright** for E2E tests
- **pnpm workspace** monorepo structure

The single source of truth for setup is `scripts/scaffold.sh`: it places the entire
self-contained `example/` (which compiles as-is), substitutes the project name, app
display name, and primary color, runs `pnpm install`, generates shadcn/ui components,
and creates the `.env.local` template — in one deterministic run. The AI executes the
script as-is instead of reimplementing the steps.

```sh
scripts/scaffold.sh --name <project> --app-name "<display name>" --primary "#RRGGBB"
# + --dest <dir> / --no-supabase / --skip-install / --dry-run / --force
```

## What Gets Generated

```
<project>/
├── package.json              # Workspace root
├── pnpm-workspace.yaml
├── .gitignore
└── apps/web/
    ├── Config files          # vite, vitest, tsconfig, eslint, shadcn, playwright
    ├── index.html
    ├── .env.local(.example)
    └── src/
        ├── main.tsx          # Entry point with global error handlers
        ├── App.tsx           # Provider stack (ErrorBoundary > Query > Auth > Router)
        ├── router.tsx        # Type-safe routes with auth guards + 404/500
        ├── index.css         # Tailwind theme (customizable colors)
        ├── auth/             # Supabase auth context
        ├── pages/            # login / home / not-found / error
        ├── components/       # Layout, shared components, shadcn/ui
        ├── data/             # Data access hooks (Supabase abstracted)
        ├── lib/              # Utilities (query-client, logger, toast, etc.)
        └── hooks/            # Generic hooks (useDebounce, etc.)
```

After scaffolding, `pnpm test`, `pnpm build`, and `pnpm lint` all pass out of the box.

## Key Patterns

### Data Access Layer

Page components never import Supabase directly. All data operations go through hooks in `src/data/<domain>/` (samples: `use-login.ts`, `use-user-profile.ts`). See `references/data-layer-pattern.md` for details.

### Auth Flow

1. `AuthProvider` recovers session on mount
2. Router `beforeLoad` guards redirect unauthenticated users to `/login`
3. Authenticated routes render within `AppLayout`

### Theming

CSS variables in `index.css` control the entire color scheme. `scaffold.sh --primary` sets `--primary` and `--ring`; change `--accent` and others to match your brand.

## Prerequisites

- Node.js 20+
- pnpm 9+
- Network access (`pnpm install` + shadcn/ui generation; use `--skip-install` to place files only)
- Supabase project (if using auth/database)

## Usage

```
/react-vite-supabase-starter
```

The skill asks for the project name, app display name, theme color, and whether to use Supabase, then runs `scripts/scaffold.sh` and verifies `pnpm test` / `pnpm build`. Without Supabase, the script emits an ACTION_REQUIRED checklist and the AI removes the auth-related code afterwards.
