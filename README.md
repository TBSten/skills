# Skills

[日本語](./README.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[Claude Code](https://docs.anthropic.com/en/docs/claude-code) skills and rules collection by TBSten.

> **Status:** 🌱 WIP · 🧪 Experimental · 🟢 Active · 💎 Active-Prime · ❌ Archived

## ⭐️ Available Skills

<table>
<tr>
<th>Skill</th>
<th>Install</th>
<th>Status</th>
<th>Description</th>
</tr>
<tr>
<td><a href="./skills/local-ticket-system.md">local-ticket-system</a></td>
<td>

```sh
gh skill install tbsten/skills local-ticket-system
```

</td>
<td>🟢 Active</td>
<td>Markdown-based local ticket management system with task, bug, and chapter tickets</td>
</tr>
<tr>
<td><a href="./skills/kotlin-tuple.md">kotlin-tuple</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-tuple
```

</td>
<td>🟢 Active</td>
<td>Type-safe Tuple utilities for Kotlin/KMP</td>
</tr>
<tr>
<td><a href="./skills/simple-loader.md">simple-loader</a></td>
<td>

```sh
gh skill install tbsten/skills simple-loader
```

</td>
<td>🟢 Active</td>
<td>Sealed interface state machine for async data loading in Kotlin/Compose Multiplatform</td>
</tr>
<tr>
<td><a href="./skills/navigation3-main-tab.md">navigation3-main-tab</a></td>
<td>

```sh
gh skill install tbsten/skills navigation3-main-tab
```

</td>
<td>🟢 Active</td>
<td>Bottom tab management pattern using Navigation 3 SceneStrategy for KMP + Compose</td>
</tr>
<tr>
<td><a href="./skills/kotlin-compiler-plugin-setup.md">kotlin-compiler-plugin-setup</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-compiler-plugin-setup
```

</td>
<td>🟢 Active</td>
<td>Set up a Kotlin Compiler Plugin project with multi-module Gradle structure, buildSrc, unit tests (kctfork), and integration tests</td>
</tr>
<tr>
<td><a href="./skills/kotlin-maven-central-publish.md">kotlin-maven-central-publish</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-maven-central-publish
```

</td>
<td>🟢 Active</td>
<td>Set up Maven Central publishing for Kotlin/KMP projects with Vanniktech Maven Publish, GPG signing, and GitHub Actions</td>
</tr>
<tr>
<td><a href="./skills/kmp-snapshot-testing-setup.md">kmp-snapshot-testing-setup</a></td>
<td>

```sh
gh skill install tbsten/skills kmp-snapshot-testing-setup
```

</td>
<td>🟢 Active</td>
<td>Set up snapshot testing infrastructure (Kotest PBT + Turbine) for KMP + Compose projects</td>
</tr>
<tr>
<td><a href="./skills/react-vite-supabase-starter.md">react-vite-supabase-starter</a></td>
<td>

```sh
gh skill install tbsten/skills react-vite-supabase-starter
```

</td>
<td>🟢 Active</td>
<td>Scaffold a React + Vite + TypeScript + Tailwind v4 + shadcn/ui + TanStack Router/Query + Supabase web app</td>
</tr>
<tr>
<td><a href="./skills/kotlin-compiler-plugin-dev.md">kotlin-compiler-plugin-dev</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-compiler-plugin-dev
```

</td>
<td>🟢 Active</td>
<td>Develop and review Kotlin Compiler Plugins using research data from 30+ existing plugins; also covers adding/removing supported Kotlin versions in projects with compat module layer or source set separation</td>
</tr>
<tr>
<td><a href="./skills/exploratory-pr-verification.md">exploratory-pr-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-pr-verification
```

</td>
<td>🟢 Active</td>
<td>Operational rules for multi-subagent exploratory PR verification on Kotlin projects (PDCA / MCP / ticket bookkeeping / PR comment etiquette / loop termination)</td>
</tr>
<tr>
<td><a href="./skills/exploratory-nightly-verification.md">exploratory-nightly-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-nightly-verification
```

</td>
<td>🟢 Active</td>
<td>60-minute single-shot exploratory verification of a Kotlin project's main branch from a nightly CI job, with Markdown-file-based findings and zero side effects</td>
</tr>
<tr>
<td><a href="./skills/pr-fix-loop.md">pr-fix-loop</a></td>
<td>

```sh
gh skill install tbsten/skills pr-fix-loop
```

</td>
<td>🟢 Active</td>
<td>Drive multiple GitHub PRs to green in parallel — one loop pass classifies each failing CI check (transient / lint / binary-compat / build / test) and delegates to a fix-ci-* skill, handles review and issue comments end to end, and chains rebases for stacked PRs; pairs with a /loop driver for unattended runs</td>
</tr>
<tr>
<td><a href="./skills/github-get-attachment-url.md">github-get-attachment-url</a></td>
<td>

```sh
gh skill install tbsten/skills github-get-attachment-url
```

</td>
<td>🟢 Active</td>
<td>Upload local files to GitHub and get their user-attachments URLs (or Markdown) without creating an issue, via a bundled deterministic Python + Playwright runner</td>
</tr>
<tr>
<td><a href="./skills/intellij-plugin-dev.md">intellij-plugin-dev</a></td>
<td>

```sh
gh skill install tbsten/skills intellij-plugin-dev
```

</td>
<td>🌱 WIP</td>
<td>Tooling & workflow reference for building IntelliJ Platform / Android Studio plugins agent-driven — decomposes verification into 6 channels anchored on headless functional tests (Kotlin Analysis API) and headless PNG self-review (renderComposeScene + Jewel); covers the tool window, gutter line markers, editor following, PSI insertion (codegen), VRT golden, Driver smoke, and build/since-until wiring</td>
</tr>
<tr>
<td><a href="./skills/status-board.md">status-board</a></td>
<td>

```sh
gh skill install tbsten/skills status-board
```

</td>
<td>🧪 Experimental</td>
<td>Collapse everything in flight — GitHub PRs and issues, local branches, and the open questions that only exist in the conversation — into one standalone HTML page: a dependency-graph SVG plus an epic-by-epic kanban, designed so "waiting on a human" and "the next move" read at a glance; ships ask boxes that collect answers in the browser, and is tuned for speed (one GraphQL round trip, defaults pre-filled, self-verification bundled into the output)</td>
</tr>
</table>

## 📝 Available Rules

<table>
<tr>
<th>Rule</th>
<th>Install</th>
<th>Status</th>
<th>Description</th>
</tr>
<tr>
<td><a href="./rules/kmp-layered-architecture.md">kmp-layered-architecture</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
```

</td>
<td>🟢 Active</td>
<td>4-layer architecture (App/UI/Domain/Data) rule for Kotlin Multiplatform + Compose projects</td>
</tr>
<tr>
<td><a href="./rules/kmp-snapshot-testing.md">kmp-snapshot-testing</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-snapshot-testing
```

</td>
<td>🟢 Active</td>
<td>Snapshot PBT testing rule for Kotlin Multiplatform projects with Kotest + Turbine</td>
</tr>
<tr>
<td><a href="./rules/kmp-error-handling.md">kmp-error-handling</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-error-handling
```

</td>
<td>🟢 Active</td>
<td>Error handling and warning detection rule for Kotlin Multiplatform + Compose projects</td>
</tr>
</table>

<details>

<summary> Installing Rules </summary>

Rules are installed via `rules/install.sh`. It downloads `RULE.md` into `.claude/rules/` and reference files into the current directory.

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- <rule-name>
```

#### Options

| Option | Description |
|---|---|
| `as=<name>` | Save the rule as `.claude/rules/<name>.md` instead of the default name |
| `--ref=<ref>` or `-r=<ref>` | Git ref (branch, tag, or commit hash) to download from (default: `main`) |

#### Examples

```sh
# Install with a custom name
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-layered-architecture as=my-architecture

# Install from a specific branch
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing --ref=feature/new-rule

# Install from a specific commit
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing -r=abc1234
```

</details>

## 🤝 Contribute Skills / Rules

Use the following skills to create a Pull Request to this repository.

<table>
<tr>
<th>Skill</th>
<th>Install</th>
<th>Status</th>
<th>Description</th>
</tr>
<tr>
<td><a href="./skills/contribute-skill.md">contribute-skill</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-skill
```

</td>
<td>🟢 Active</td>
<td>Package project knowledge as a skill and create a PR to TBSten/skills</td>
</tr>
<tr>
<td><a href="./skills/contribute-rule.md">contribute-rule</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-rule
```

</td>
<td>🟢 Active</td>
<td>Package project knowledge as a rule and create a PR to TBSten/skills</td>
</tr>
</table>
