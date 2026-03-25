# Skills

[日本語](./README.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

[Claude Code](https://docs.anthropic.com/en/docs/claude-code) skills and rules collection by TBSten.

## Available Skills

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
npx skills add tbsten/skills --skill kotlin-tuple
```

</td>
<td>Type-safe Tuple utilities (Tuple0–Tuple20) for Kotlin/KMP</td>
<td><a href="./kotlin-tuple.md">kotlin-tuple.md</a></td>
</tr>
<tr>
<td>contribute-skill</td>
<td>

```sh
npx skills add tbsten/skills --skill contribute-skill
```

</td>
<td>Package project knowledge as a skill and create a PR to TBSten/skills</td>
<td><a href="./contribute-skill.md">contribute-skill.md</a></td>
</tr>
<tr>
<td>contribute-rule</td>
<td>

```sh
npx skills add tbsten/skills --skill contribute-rule
```

</td>
<td>Package project knowledge as a rule and create a PR to TBSten/skills</td>
<td><a href="./contribute-rule.md">contribute-rule.md</a></td>
</tr>
<tr>
<td>simple-loader</td>
<td>

```sh
npx skills add tbsten/skills --skill simple-loader
```

</td>
<td>Sealed interface state machine for async data loading in Kotlin/Compose Multiplatform</td>
<td><a href="./simple-loader.md">simple-loader.md</a></td>
</tr>
</table>

## Installing Rules

Rules are installed via `rules/install.sh`. It downloads `RULE.md` into `.claude/rules/` and reference files into the current directory.

```sh
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- <rule-name>
```

### Options

| Option | Description |
|---|---|
| `as=<name>` | Save the rule as `.claude/rules/<name>.md` instead of the default name |
| `--ref=<ref>` or `-r=<ref>` | Git ref (branch, tag, or commit hash) to download from (default: `main`) |

### Examples

```sh
# Install with a custom name
curl -fsSL .../rules/install.sh | bash -s -- kmp-layered-architecture as=my-architecture

# Install from a specific branch
curl -fsSL .../rules/install.sh | bash -s -- kmp-snapshot-testing --ref=feature/new-rule

# Install from a specific commit
curl -fsSL .../rules/install.sh | bash -s -- kmp-snapshot-testing -r=abc1234
```

## Available Rules

<table>
<tr>
<th>Rule</th>
<th>Install</th>
<th>Description</th>
<th>Details</th>
</tr>
</table>
