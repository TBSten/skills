# react-vite-supabase-starter

Scaffold a new web application with React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase.

## Install

```sh
npx skills add tbsten/skills \
  --skill react-vite-supabase-starter
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

## What Gets Generated

```
<project>/
├── package.json              # Workspace root
├── pnpm-workspace.yaml
└── apps/web/
    ├── Config files          # vite, tsconfig, eslint, shadcn, playwright
    └── src/
        ├── main.tsx          # Entry point with global error handlers
        ├── App.tsx           # Provider stack (ErrorBoundary > Query > Auth > Router)
        ├── router.tsx        # Type-safe routes with auth guards
        ├── index.css         # Tailwind theme (customizable colors)
        ├── auth/             # Supabase auth context
        ├── components/       # Layout, shared components, shadcn/ui
        ├── data/             # Data access hooks (Supabase abstracted)
        ├── lib/              # Utilities (query-client, logger, toast, etc.)
        └── hooks/            # Generic hooks (useDebounce, etc.)
```

## Key Patterns

### Data Access Layer

Page components never import Supabase directly. All data operations go through hooks in `src/data/<domain>/`. See `references/data-layer-pattern.md` for details.

### Auth Flow

1. `AuthProvider` recovers session on mount
2. Router `beforeLoad` guards redirect unauthenticated users to `/login`
3. Authenticated routes render within `AppLayout`

### Theming

CSS variables in `index.css` control the entire color scheme. Change `--primary`, `--accent`, and `--ring` to match your brand.

## Prerequisites

- Node.js 20+
- pnpm 9+
- Supabase project (if using auth/database)

## Usage

```
/react-vite-supabase-starter
```

The skill will ask for project name, app display name, and theme color, then generate the full project scaffold.
