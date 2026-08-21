---
status: Experimental
group: Kotlin ライブラリ/ツール開発
---

# ksp-plugin-setup Prompt

[日本語](./ksp-plugin-setup.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

One-shot prompt version of the [ksp-plugin-setup skill](../skills/ksp-plugin-setup.md). Scaffolds a
KSP plugin (Symbol Processor) project in the layout [cream.kt](https://github.com/TBSten/cream)
arrived at — a runtime / ksp / test module split, a four-layer processor, a golden-file test harness,
and architecture rules installed into the generated project — without installing any skill.

## Run

Paste the following into Claude Code (or any coding agent):

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/ksp-plugin-setup/PROMPT.md and follow its instructions
```

## What it does

- Confirms project name, package / group ID, the first annotation name, setup scope and Kotlin/KSP versions before starting
- Sparse-clones the repository and runs `scripts/scaffold.sh`, which deterministically copies `example/`, remaps directories into source sets, substitutes every placeholder, renames the `Greeting*` / `Example*` files, places the `META-INF/services` provider entry and installs five path-scoped `.claude/rules/*.md` files — the agent never hand-copies or hand-substitutes
- The scaffolded project contains: the Gradle foundation (version catalog as single source of truth, `buildLogic` included build, `ksp.incremental=false`), the runtime module (annotation declarations only, every KMP target), the JVM-only ksp module (three root files plus `feature/` · `core/` · `options/` · `util/` with one-way dependencies), the `test` module with the KSP × KMP workaround, the kctfork + golden + Konsist test harness, and a GitHub Actions matrix CI
- Reviews the placed-file list and the layering, then runs `scripts/verify.sh` (the four build checks, logs under `.local/tmp/`) and records and reads the first goldens

## Referenced files

The prompt sparse-clones these from GitHub instead of a local skill install:

- [skills/ksp-plugin-setup/scripts/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/scripts) — `scaffold.sh` (copy / substitute / rename, the spec's single source of truth) and `verify.sh` (build checks)
- [skills/ksp-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/example) — build files, processor skeleton, test infrastructure, CI workflows
- [skills/ksp-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/references) — build & CI details, processor design decisions, test infrastructure guide
- [skills/ksp-plugin-setup/assets/rules/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/assets/rules) — the `.claude/rules/*.md` templates

## Related

- Skill version: [skills/ksp-plugin-setup](../skills/ksp-plugin-setup.md) — install with `gh skill install tbsten/skills ksp-plugin-setup`
- [kotlin-maven-central-publish](./kotlin-maven-central-publish.md) — the full Maven Central setup this prompt delegates to
