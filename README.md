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

## Available Rules

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
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- kmp-layered-architecture
```

</td>
<td>4-layer architecture (App/UI/Domain/Data) rule for Kotlin Multiplatform + Compose projects</td>
<td><a href="./rules/kmp-layered-architecture/RULE.md">RULE.md</a></td>
</tr>
<tr>
<td>kmp-testing</td>
<td>

```sh
curl -fsSL https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh | bash -s -- kmp-testing
```

</td>
<td>Testing strategy rule (unit tests + snapshot PBT) for Kotlin Multiplatform projects with Kotest + Turbine</td>
<td><a href="./rules/kmp-testing/RULE.md">RULE.md</a></td>
</tr>
</table>
