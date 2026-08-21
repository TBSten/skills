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

- Sparse-clones the skills repo, then runs the bundled `scripts/install.sh` as-is
  (`--project` / `--package` required; `--module-path`, `--ui-module-path`, `--skip-compose`,
  `--dry-run`, `--force` optional). The idempotent script:
  - Adds Kotest / Turbine / kotlinx-serialization / coroutines-test entries to `gradle/libs.versions.toml`
  - Copies build-logic convention plugins (`convention-kmp-test`, `convention-kmp-snapshot-testing`, `SnapshotReportTask`) with ProjectConfig FQCN / module path substituted
  - Creates the core testing module (build.gradle.kts + 26 sources: `shouldMatchSnapshot`, `StateHolderSnapshotPbtSpec`, `LogicSnapshotPbtSpec`, `KotlinCodeFormat`, PBT utilities) and the Compose testing module (build.gradle.kts + `ComposeSnapshotPbtSpec`, `runComposableSnapshotTest`), with `settings.gradle.kts` includes and package renaming
  - Installs shell scripts for the snapshot diff workflow (`tools/snapshot-diff.sh` + step scripts)
  - Registers the `cleanSnapshotOutputDir` task in the root `build.gradle.kts`
  - Emits a one-line result JSON (conflicts / warnings / manual follow-ups)
- The agent then reviews the JSON, handles project-specific follow-ups (serialization plugin classpath in build-logic, `convention-kmp` mapping, `AppTheme` / `WithTestGraph` adaptation), applies the plugin to target modules, and verifies the build with `./gradlew compileKotlinJvm`

## Referenced files

The prompt fetches these from GitHub instead of a local skill install:

- [skills/kmp-snapshot-testing-setup/scripts/install.sh](https://github.com/TBSten/skills/blob/main/skills/kmp-snapshot-testing-setup/scripts/install.sh) — the install script that places and substitutes everything below
- [skills/kmp-snapshot-testing-setup/example/build-logic/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/build-logic) — convention plugins and the report task
- [skills/kmp-snapshot-testing-setup/example/core-testing-snapshot/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/core-testing-snapshot) — core testing module build file template + sources (26 files)
- [skills/kmp-snapshot-testing-setup/example/ui-core-testing/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/ui-core-testing) — Compose PBT testing module build file template + sources
- [skills/kmp-snapshot-testing-setup/example/tools/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/example/tools) — snapshot-diff shell scripts
- [skills/kmp-snapshot-testing-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kmp-snapshot-testing-setup/references) — architecture documentation

## Related

- Skill version: [skills/kmp-snapshot-testing-setup](../skills/kmp-snapshot-testing-setup.md) — install with `gh skill install tbsten/skills kmp-snapshot-testing-setup`
