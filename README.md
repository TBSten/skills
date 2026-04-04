# Skills

[日本語](./README.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[Claude Code](https://docs.anthropic.com/en/docs/claude-code) skills and rules collection by TBSten.

## ⭐️ Available Skills

<table>
<tr>
<th>Skill</th>
<th>Install</th>
<th>Description</th>
</tr>
<tr>
<td><a href="./skills/local-ticket-system.md">local-ticket-system</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill local-ticket-system
```

</td>
<td>Markdown-based local ticket management system with task, bug, and chapter tickets</td>
</tr>
<tr>
<td><a href="./skills/kotlin-tuple.md">kotlin-tuple</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-tuple
```

</td>
<td>Type-safe Tuple utilities for Kotlin/KMP</td>
</tr>
<tr>
<td><a href="./skills/simple-loader.md">simple-loader</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill simple-loader
```

</td>
<td>Sealed interface state machine for async data loading in Kotlin/Compose Multiplatform</td>
</tr>
<tr>
<td><a href="./skills/navigation3-main-tab.md">navigation3-main-tab</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill navigation3-main-tab
```

</td>
<td>Bottom tab management pattern using Navigation 3 SceneStrategy for KMP + Compose</td>
</tr>
<tr>
<td><a href="./skills/kotlin-compiler-plugin-setup.md">kotlin-compiler-plugin-setup</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-compiler-plugin-setup
```

</td>
<td>Set up a Kotlin Compiler Plugin project with multi-module Gradle structure, buildSrc, unit tests (kctfork), and integration tests</td>
</tr>
<tr>
<td><a href="./skills/kotlin-maven-central-publish.md">kotlin-maven-central-publish</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-maven-central-publish
```

</td>
<td>Set up Maven Central publishing for Kotlin/KMP projects with Vanniktech Maven Publish, GPG signing, and GitHub Actions</td>
</tr>
<tr>
<td><a href="./skills/kmp-snapshot-testing-setup.md">kmp-snapshot-testing-setup</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kmp-snapshot-testing-setup
```

</td>
<td>Set up snapshot testing infrastructure (Kotest PBT + Turbine) for KMP + Compose projects</td>
</tr>
<tr>
<td><a href="./skills/react-vite-supabase-starter.md">react-vite-supabase-starter</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill react-vite-supabase-starter
```

</td>
<td>Scaffold a React + Vite + TypeScript + Tailwind v4 + shadcn/ui + TanStack Router/Query + Supabase web app</td>
</tr>
</table>

## 📝 Available Rules

<table>
<tr>
<th>Rule</th>
<th>Install</th>
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
<td>Snapshot PBT testing rule for Kotlin Multiplatform projects with Kotest + Turbine</td>
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
<th>Description</th>
</tr>
<tr>
<td><a href="./skills/contribute-skill.md">contribute-skill</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-skill
```

</td>
<td>Package project knowledge as a skill and create a PR to TBSten/skills</td>
</tr>
<tr>
<td><a href="./skills/contribute-rule.md">contribute-rule</a></td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-rule
```

</td>
<td>Package project knowledge as a rule and create a PR to TBSten/skills</td>
</tr>
</table>
