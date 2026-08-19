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
- Creates the Gradle foundation: `settings.gradle.kts`, a version catalog holding the project's own version, `buildLogic` as an included build sharing that catalog, and `gradle.properties` with `ksp.incremental=false`
- Creates the runtime module (annotation declarations only, every KMP target, `explicitApi()`) and the JVM-only ksp module with `-Xcontext-parameters`
- Scaffolds the processor as three root files plus `feature/` · `core/` · `options/` · `util/`, with one-way dependencies and layer-narrowed context parameters
- Creates the `test` module with the KSP × KMP workaround, keeping the `*Test` KSP tasks enabled for kotest's per-target launchers
- Copies the test harness: kctfork end-to-end compilation, facet-based Markdown goldens, generator-driven snapshot matrices, diagnostic goldens, and Konsist architecture specs
- Adds a GitHub Actions matrix CI (and a Release-triggered publish workflow)
- Installs five path-scoped `.claude/rules/*.md` files into the generated project so the layering stays enforced afterwards
- Verifies the build and records the first goldens

## Referenced files

The prompt fetches these from GitHub instead of a local skill install:

- [skills/ksp-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/example) — build files, processor skeleton, test infrastructure, CI workflows
- [skills/ksp-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/references) — build & CI details, processor design decisions, test infrastructure guide
- [skills/ksp-plugin-setup/assets/rules/](https://github.com/TBSten/skills/tree/main/skills/ksp-plugin-setup/assets/rules) — the `.claude/rules/*.md` templates

## Related

- Skill version: [skills/ksp-plugin-setup](../skills/ksp-plugin-setup.md) — install with `gh skill install tbsten/skills ksp-plugin-setup`
- [kotlin-maven-central-publish](./kotlin-maven-central-publish.md) — the full Maven Central setup this prompt delegates to
