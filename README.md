# Skills

[日本語](./README.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[Claude Code](https://docs.anthropic.com/en/docs/claude-code) skills and rules collection by TBSten.

> **Status:** 🌱 WIP · 🧪 Experimental · ✅ Active · 💎 Active-Prime · ❌ Archived

## 🧩 Install as Plugins

This repository is also a Claude Code plugin marketplace (`tbsten-skills`). Skills are bundled into one plugin per group; installing a plugin installs every non-archived skill in that group. Skills are invoked as `<plugin>:<skill>`.

```sh
/plugin marketplace add TBSten/skills
/plugin install kotlin-library-dev@tbsten-skills
```

On Claude Code 2.1.275 or later, the single line in the table below adds the marketplace and installs in one step. Rules and prompts are not part of the plugins; use them as described in their sections.

<table>
<tr>
<th>Group</th>
<th>Plugin</th>
<th>Install</th>
</tr>
<tr>
<td>🔴 Task Management</td>
<td><code>task-management</code></td>
<td>

```sh
/plugin install task-management --marketplace TBSten/skills
```

</td>
</tr>
<tr>
<td>🟢 Kotlin / Android App Development</td>
<td><code>kotlin-android-app</code></td>
<td>

```sh
/plugin install kotlin-android-app --marketplace TBSten/skills
```

</td>
</tr>
<tr>
<td>🟣 Kotlin Library / Tool Development</td>
<td><code>kotlin-library-dev</code></td>
<td>

```sh
/plugin install kotlin-library-dev --marketplace TBSten/skills
```

</td>
</tr>
<tr>
<td>🔵 Web Frontend</td>
<td><code>web-frontend</code></td>
<td>

```sh
/plugin install web-frontend --marketplace TBSten/skills
```

</td>
</tr>
<tr>
<td>⚫️ Git / GitHub</td>
<td><code>git-github</code></td>
<td>

```sh
/plugin install git-github --marketplace TBSten/skills
```

</td>
</tr>
<tr>
<td>🟡 Testing / Verification</td>
<td><code>testing-verification</code></td>
<td>

```sh
/plugin install testing-verification --marketplace TBSten/skills
```

</td>
</tr>
<tr>
<td>🤝 Contribute skills</td>
<td><code>contribute</code></td>
<td>

```sh
/plugin install contribute --marketplace TBSten/skills
```

</td>
</tr>
</table>

## ⭐️ Available Skills

<table>
<tr>
<th>Group</th>
<th>Skill</th>
<th>Install</th>
<th>Status</th>
<th>Description</th>
</tr>
<tr>
<td>🔴 Task Management</td>
<td><a href="./skills/local-ticket-system.md">local-ticket-system</a></td>
<td>

```sh
gh skill install tbsten/skills local-ticket-system
```

</td>
<td>✅ Active</td>
<td>Markdown-based local ticket management with task, bug, and chapter tickets</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/status-board.md">status-board</a></td>
<td>

```sh
gh skill install tbsten/skills status-board
```

</td>
<td>🧪 Experimental</td>
<td>Put PRs, issues, branches and open questions in one dependency graph + kanban</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/pm.md">pm</a></td>
<td>

```sh
gh skill install tbsten/skills pm
```

</td>
<td>🧪 Experimental</td>
<td>Diagnose PM trouble, advise, and draft only the docs that help decide (46 kinds)</td>
</tr>
<tr>
<td>🟢 Kotlin / Android App Development</td>
<td><a href="./skills/kotlin-tuple.md">kotlin-tuple</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-tuple
```

</td>
<td>🧪 Experimental</td>
<td>Gradle setup that generates type-safe Tuple utilities for Kotlin/KMP</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/simple-loader.md">simple-loader</a></td>
<td>

```sh
gh skill install tbsten/skills simple-loader
```

</td>
<td>🧪 Experimental</td>
<td>Sealed interface based async data loading state management for Kotlin/Compose MP</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/navigation3-main-tab.md">navigation3-main-tab</a></td>
<td>

```sh
gh skill install tbsten/skills navigation3-main-tab
```

</td>
<td>🧪 Experimental</td>
<td>Bottom tab management pattern using Navigation 3 SceneStrategy for KMP + Compose</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/kmp-snapshot-testing-setup.md">kmp-snapshot-testing-setup</a></td>
<td>

```sh
gh skill install tbsten/skills kmp-snapshot-testing-setup
```

</td>
<td>🧪 Experimental</td>
<td>Snapshot testing infrastructure for KMP + Compose projects</td>
</tr>
<tr>
<td>🟣 Kotlin Library / Tool Development</td>
<td><a href="./skills/kotlin-compiler-plugin.md">kotlin-compiler-plugin</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-compiler-plugin
```

</td>
<td>🧪 Experimental</td>
<td>Setup, dev, review and Kotlin version support for compiler plugins</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/kotlin-maven-central-publish.md">kotlin-maven-central-publish</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-maven-central-publish
```

</td>
<td>✅ Active</td>
<td>Maven Central publishing setup for Kotlin/KMP</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/intellij-plugin-dev.md">intellij-plugin-dev</a></td>
<td>

```sh
gh skill install tbsten/skills intellij-plugin-dev
```

</td>
<td>🌱 WIP</td>
<td>Tooling and verification-channel reference for agent-driven IntelliJ plugin dev</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/kotlin-library-bootup.md">kotlin-library-bootup</a></td>
<td>

```sh
gh skill install tbsten/skills kotlin-library-bootup
```

</td>
<td>🧪 Experimental</td>
<td>Bootstrap a Kotlin/KMP library with CI, publishing and docs</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/ksp-plugin-setup.md">ksp-plugin-setup</a></td>
<td>

```sh
gh skill install tbsten/skills ksp-plugin-setup
```

</td>
<td>🧪 Experimental</td>
<td>Scaffold a KSP plugin with a 3-module layout</td>
</tr>
<tr>
<td>🔵 Web Frontend</td>
<td><a href="./skills/react-vite-supabase-starter.md">react-vite-supabase-starter</a></td>
<td>

```sh
gh skill install tbsten/skills react-vite-supabase-starter
```

</td>
<td>🧪 Experimental</td>
<td>Scaffold a React + Vite + Tailwind v4 + shadcn/ui + TanStack + Supabase web app</td>
</tr>
<tr>
<td>⚫️ Git / GitHub</td>
<td><a href="./skills/exploratory-pr-verification.md">exploratory-pr-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-pr-verification
```

</td>
<td>🧪 Experimental</td>
<td>Rules for multi-subagent exploratory PR verification on Kotlin projects</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/exploratory-nightly-verification.md">exploratory-nightly-verification</a></td>
<td>

```sh
gh skill install tbsten/skills exploratory-nightly-verification
```

</td>
<td>🧪 Experimental</td>
<td>60-min single-shot exploratory verification of main from a nightly CI job</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/pr-fix-loop.md">pr-fix-loop</a></td>
<td>

```sh
gh skill install tbsten/skills pr-fix-loop
```

</td>
<td>✅ Active</td>
<td>Drive many PRs to green: classify CI failures, handle comments, chain rebases</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/visual-explanation.md">visual-explanation</a></td>
<td>

```sh
gh skill install tbsten/skills visual-explanation
```

</td>
<td>🧪 Experimental</td>
<td>Add a first diagram/screenshot to PRs & tickets: capture, attach by SHA, check</td>
</tr>
<tr>
<td>🟡 Testing / Verification</td>
<td><a href="./skills/check-method-level.md">check-method-level</a></td>
<td>

```sh
gh skill install tbsten/skills check-method-level
```

</td>
<td>🧪 Experimental</td>
<td>Score how strong a test / verification method is on 9 axes via a script</td>
</tr>
<tr>
<td></td>
<td><a href="./skills/feedback-mode.md">feedback-mode</a></td>
<td>

```sh
gh skill install tbsten/skills feedback-mode
```

</td>
<td>🧪 Experimental</td>
<td>Add a debug-only in-app feedback mode and let Claude Code work through it</td>
</tr>
</table>

## 📝 Available Rules

<table>
<tr>
<th>Group</th>
<th>Rule</th>
<th>Install</th>
<th>Status</th>
<th>Description</th>
</tr>
<tr>
<td>🟢 Kotlin / Android App Development</td>
<td><a href="./rules/kmp-snapshot-testing.md">kmp-snapshot-testing</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-snapshot-testing
```

</td>
<td>🧪 Experimental</td>
<td>Snapshot PBT testing rule for Kotlin Multiplatform projects</td>
</tr>
<tr>
<td></td>
<td><a href="./rules/kmp-error-handling.md">kmp-error-handling</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-error-handling
```

</td>
<td>🧪 Experimental</td>
<td>Error handling and warning management for KMP + Compose projects</td>
</tr>
<tr>
<td></td>
<td><a href="./rules/kmp-layered-architecture.md">kmp-layered-architecture</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
```

</td>
<td>🧪 Experimental</td>
<td>4-layer architecture for KMP + Compose projects</td>
</tr>
<tr>
<td>⚫️ Git / GitHub</td>
<td><a href="./rules/visual-explanation.md">visual-explanation</a></td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- visual-explanation
```

</td>
<td>🧪 Experimental</td>
<td>Make PR / ticket explanations start with a diagram, screenshot or real output</td>
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

## 💬 Available Prompts

One-shot prompts that need no skill installation. Copy the prompt in the Run column and paste it into Claude Code.

<table>
<tr>
<th>Group</th>
<th>Prompt</th>
<th>Run</th>
<th>Status</th>
<th>Description</th>
</tr>
<tr>
<td>🟢 Kotlin / Android App Development</td>
<td><a href="./prompts/kmp-snapshot-testing-setup.md">kmp-snapshot-testing-setup</a></td>
<td>

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kmp-snapshot-testing-setup/PROMPT.md and follow its instructions
```

</td>
<td>🧪 Experimental</td>
<td>Set up KMP + Compose snapshot testing (prompt version of the skill)</td>
</tr>
<tr>
<td>🟣 Kotlin Library / Tool Development</td>
<td><a href="./prompts/kotlin-compiler-plugin-setup.md">kotlin-compiler-plugin-setup</a></td>
<td>

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-compiler-plugin-setup/PROMPT.md and follow its instructions
```

</td>
<td>🧪 Experimental</td>
<td>Set up a Kotlin Compiler Plugin project (prompt version of the skill)</td>
</tr>
<tr>
<td></td>
<td><a href="./prompts/kotlin-maven-central-publish.md">kotlin-maven-central-publish</a></td>
<td>

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-maven-central-publish/PROMPT.md and follow its instructions
```

</td>
<td>🧪 Experimental</td>
<td>Set up Maven Central publishing for Kotlin/KMP (prompt version of the skill)</td>
</tr>
<tr>
<td></td>
<td><a href="./prompts/ksp-plugin-setup.md">ksp-plugin-setup</a></td>
<td>

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/ksp-plugin-setup/PROMPT.md and follow its instructions
```

</td>
<td>🧪 Experimental</td>
<td>Scaffold a KSP plugin project (prompt version of the skill)</td>
</tr>
<tr>
<td>🔵 Web Frontend</td>
<td><a href="./prompts/react-vite-supabase-starter.md">react-vite-supabase-starter</a></td>
<td>

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/react-vite-supabase-starter/PROMPT.md and follow its instructions
```

</td>
<td>🧪 Experimental</td>
<td>Scaffold a React + Vite + Supabase web app (prompt version of the skill)</td>
</tr>
</table>

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
<td>✅ Active</td>
<td>Package project knowledge as a skill and create a PR to TBSten/skills</td>
</tr>
<tr>
<td><a href="./skills/contribute-rule.md">contribute-rule</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-rule
```

</td>
<td>✅ Active</td>
<td>Package project knowledge as a rule and create a PR to TBSten/skills</td>
</tr>
<tr>
<td><a href="./skills/contribute-prompt.md">contribute-prompt</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-prompt
```

</td>
<td>🧪 Experimental</td>
<td>Package project knowledge as a prompt and create a PR to TBSten/skills</td>
</tr>
<tr>
<td><a href="./skills/contribute-batch.md">contribute-batch</a></td>
<td>

```sh
gh skill install tbsten/skills contribute-batch
```

</td>
<td>🧪 Experimental</td>
<td>Triage knowledge into skills / rules / prompts and open one PR to TBSten/skills</td>
</tr>
</table>

<details>

<summary> ❌ Archived Skills / Rules / Prompts </summary>

<table>
<tr>
<th>Name</th>
<th>Install / Run</th>
<th>Status</th>
<th>Description</th>
</tr>
<tr>
<td><a href="./skills/github-get-attachment-url.md">github-get-attachment-url</a></td>
<td>

```sh
gh skill install tbsten/skills github-get-attachment-url
```

</td>
<td>❌ Archived</td>
<td>Upload local files to GitHub (superseded by GitHub CLI v2.99.0's <code>--attach</code> flag)</td>
</tr>
</table>

</details>
