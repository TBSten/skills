---
status: Experimental
group: Web フロントエンド
---

# react-vite-supabase-starter Prompt

[日本語](./react-vite-supabase-starter.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

One-shot prompt version of the [react-vite-supabase-starter skill](../skills/react-vite-supabase-starter.md). Scaffolds a new web application on the React + Vite + TypeScript + Tailwind CSS v4 + shadcn/ui + TanStack Router + TanStack Query + Supabase stack as a pnpm workspace monorepo, with auth, routing, a data access layer, layouts, and a test setup — no skill install required.

## Run

Paste the following into Claude Code (or any coding agent):

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/react-vite-supabase-starter/PROMPT.md and follow its instructions
```

## What it does

- Asks for the project name, app display name, theme color, and whether to use Supabase
- Creates a pnpm workspace monorepo with a Vite + React + TypeScript (strict) app under `apps/web`
- Configures Tailwind CSS v4 theming (CSS variables, customizable `--primary`) and shadcn/ui components
- Wires TanStack Router (type-safe routes with auth guards) and TanStack Query
- Adds a Supabase auth context and a `src/data/<domain>/` data access layer that hides Supabase from page components (optional)
- Places layout and shared components, error handling utilities, and sample pages (`/login`, `/`, 404/500)
- Sets up Vitest + Testing Library and Playwright, then verifies `pnpm test` and `pnpm build` pass
- Fetches all reference files from GitHub (sparse clone recommended) instead of a local skill install

## Referenced files

The prompt fetches these from GitHub instead of a local skill install:

- [skills/react-vite-supabase-starter/example/config/](https://github.com/TBSten/skills/tree/main/skills/react-vite-supabase-starter/example/config) — Config file examples (vite, tsconfig, eslint, vitest, playwright, shadcn `components.json`)
- [skills/react-vite-supabase-starter/example/src/](https://github.com/TBSten/skills/tree/main/skills/react-vite-supabase-starter/example/src) — Source examples (entry points, auth context, layout/shared components, `lib/` utilities, theme CSS, test setup)
- [skills/react-vite-supabase-starter/references/data-layer-pattern.md](https://github.com/TBSten/skills/blob/main/skills/react-vite-supabase-starter/references/data-layer-pattern.md) — Data access layer pattern (query/mutation hooks, query key conventions)

## Related

- Skill version: [skills/react-vite-supabase-starter](../skills/react-vite-supabase-starter.md) — install with `gh skill install tbsten/skills react-vite-supabase-starter`
