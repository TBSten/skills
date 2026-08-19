---
status: Experimental
group: Kotlin / Android アプリ開発
---

# kmp-snapshot-testing-setup Prompt

[日本語](./kmp-snapshot-testing-setup.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

One-shot prompt version of the [kmp-snapshot-testing-setup skill](../skills/kmp-snapshot-testing-setup.md). It sets up snapshot testing infrastructure for Kotlin Multiplatform + Compose projects (convention plugins, Kotest PBT base classes, and snapshot-diff shell scripts) without installing the skill.

## Run

Paste the following into Claude Code (or any coding agent):

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kmp-snapshot-testing-setup/PROMPT.md and follow its instructions
```

## What it does

- Adds Kotest / Turbine / coroutines-test entries to `gradle/libs.versions.toml`
- Creates build-logic convention plugins (`convention-kmp-test`, `convention-kmp-snapshot-testing`, `SnapshotReportTask`)
- Creates a core testing module (`shouldMatchSnapshot`, `StateHolderSnapshotPbtSpec`, `LogicSnapshotPbtSpec`, `KotlinCodeFormat`, PBT utilities)
- Creates a Compose testing module (`ComposeSnapshotPbtSpec`, `runComposableSnapshotTest`)
- Installs shell scripts for the snapshot diff workflow (`tools/snapshot-diff.sh` + step scripts)
- Registers the `cleanSnapshotOutputDir` task in the root `build.gradle.kts`, applies the plugin to target modules, and verifies the build with `./gradlew compileKotlinJvm`

## Referenced files

The prompt fetches these from GitHub instead of a local skill install:

- [skills/kmp-snapshot-testing-setup/example/build-logic/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/build-logic) — convention plugins and the report task
- [skills/kmp-snapshot-testing-setup/example/core-testing-snapshot/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/core-testing-snapshot) — core testing module sources (25 files)
- [skills/kmp-snapshot-testing-setup/example/ui-core-testing/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/ui-core-testing) — Compose PBT testing module sources
- [skills/kmp-snapshot-testing-setup/example/tools/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/tools) — snapshot-diff shell scripts
- [skills/kmp-snapshot-testing-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/references) — architecture documentation

## Related

- Skill version: [skills/kmp-snapshot-testing-setup](../skills/kmp-snapshot-testing-setup.md) — install with `gh skill install tbsten/skills kmp-snapshot-testing-setup`
