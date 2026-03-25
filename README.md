# Skills

[日本語](./README.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[Claude Code](https://docs.anthropic.com/en/docs/claude-code) skills and rules collection by TBSten.

## ⭐️ Available Skills

<table>
<tr>
<th>Skill</th>
<th>Install</th>
<th>Description</th>
<th>Details</th>
</tr>
<tr>
<td>kotlin-tuple</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kotlin-tuple
```

</td>
<td>Type-safe Tuple utilities for Kotlin/KMP</td>
<td><a href="./skills/kotlin-tuple.md">Details</a></td>
</tr>
<tr>
<td>contribute-skill</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-skill
```

</td>
<td>Package project knowledge as a skill and create a PR to TBSten/skills</td>
<td><a href="./skills/contribute-skill.md">Details</a></td>
</tr>
<tr>
<td>contribute-rule</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill contribute-rule
```

</td>
<td>Package project knowledge as a rule and create a PR to TBSten/skills</td>
<td><a href="./skills/contribute-rule.md">Details</a></td>
</tr>
<tr>
<td>simple-loader</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill simple-loader
```

</td>
<td>Sealed interface state machine for async data loading in Kotlin/Compose Multiplatform</td>
<td><a href="./skills/simple-loader.md">Details</a></td>
</tr>
<tr>
<td>kmp-snapshot-testing-setup</td>
<td>

```sh
npx skills add tbsten/skills \
  --skill kmp-snapshot-testing-setup
```

</td>
<td>Set up snapshot testing infrastructure (Kotest PBT + Turbine) for KMP + Compose projects</td>
<td><a href="./skills/kmp-snapshot-testing-setup.md">Details</a></td>
</tr>
</table>

## 📝 Available Rules

<table>
<tr>
<th>Rule</th>
<th>Install</th>
<th>Description</th>
<th>Details</th>
</tr>
<tr>
<td>kmp-layered-architecture</td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-layered-architecture
```

</td>
<td>4-layer architecture (App/UI/Domain/Data) rule for Kotlin Multiplatform + Compose projects</td>
<td><a href="./rules/kmp-layered-architecture.md">Details</a></td>
</tr>
<tr>
<td>kmp-snapshot-testing</td>
<td>

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- kmp-snapshot-testing
```

</td>
<td>Snapshot PBT testing rule for Kotlin Multiplatform projects with Kotest + Turbine</td>
<td><a href="./rules/kmp-snapshot-testing.md">Details</a></td>
</tr>
</table>

## Installing Rules

Rules are installed via `rules/install.sh`. It downloads `RULE.md` into `.claude/rules/` and reference files into the current directory.

```sh
curl -fsSL https://rules.tbsten.me/i | \
  bash -s -- <rule-name>
```

### Options

| Option | Description |
|---|---|
| `as=<name>` | Save the rule as `.claude/rules/<name>.md` instead of the default name |
| `--ref=<ref>` or `-r=<ref>` | Git ref (branch, tag, or commit hash) to download from (default: `main`) |

### Examples

```sh
# Install with a custom name
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-layered-architecture as=my-architecture

# Install from a specific branch
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing --ref=feature/new-rule

# Install from a specific commit
curl -fsSL https://rules.tbsten.me/i | bash -s -- kmp-snapshot-testing -r=abc1234
```
