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

- Fetches the skill (scaffold script + self-contained `example/`) via a sparse clone of this repository
- Asks for the project name, app display name, theme color, and whether to use Supabase
- Runs `scripts/scaffold.sh` as-is (no reimplementation of the steps): it places all files, substitutes the project name / app name / primary color, runs `pnpm install`, generates shadcn/ui components with `pnpm dlx shadcn@latest add`, and creates the `.env.local` template
- Verifies `pnpm test` and `pnpm build` pass (both are green out of the box, as is `pnpm lint`)
- Without Supabase, follows the script's ACTION_REQUIRED checklist to remove the auth-related code
- Then adjusts the project to the user's requirements (data access hooks, theme variables, pages/routes)

## Referenced files

The prompt fetches the whole skill directory from GitHub via sparse clone instead of a local skill install:

- [skills/react-vite-supabase-starter/scripts/scaffold.sh](https://github.com/TBSten/skills/blob/main/skills/react-vite-supabase-starter/scripts/scaffold.sh) — Scaffold script (single source of truth for setup)
- [skills/react-vite-supabase-starter/example/](https://github.com/TBSten/skills/tree/main/skills/react-vite-supabase-starter/example) — Self-contained template: `root/` (workspace root files), `web/` (app package.json, index.html, env template), `config/` (vite, tsconfig, eslint, vitest, playwright, shadcn `components.json`), `src/` (entry points, router with auth guards, auth context, pages, layout/shared components, data access hooks, `lib/` utilities, theme CSS, sample test)
- [skills/react-vite-supabase-starter/references/data-layer-pattern.md](https://github.com/TBSten/skills/blob/main/skills/react-vite-supabase-starter/references/data-layer-pattern.md) — Data access layer pattern (query/mutation hooks, query key conventions)

## Related

- Skill version: [skills/react-vite-supabase-starter](../skills/react-vite-supabase-starter.md) — install with `gh skill install tbsten/skills react-vite-supabase-starter`
