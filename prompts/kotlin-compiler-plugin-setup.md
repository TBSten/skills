---
status: Experimental
group: Kotlin ライブラリ/ツール開発
---

# kotlin-compiler-plugin-setup Prompt

[日本語](./kotlin-compiler-plugin-setup.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

One-shot prompt version of the [kotlin-compiler-plugin-setup skill](../skills/kotlin-compiler-plugin-setup.md). Sets up a Kotlin Compiler Plugin project with a full multi-module Gradle structure — buildSrc convention plugins, compiler-plugin (FIR + IR), Gradle plugin wrapper, Kotlin Multiplatform runtime, unit tests (kctfork + Kotest), and integration tests — without installing any skill.

## Run

Paste the following into Claude Code (or any coding agent):

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-compiler-plugin-setup/PROMPT.md and follow its instructions
```

## What it does

- Confirms project name, group ID, plugin ID, setup scope, and Kotlin/Java versions before starting
- Sparse-clones the repo and runs `scripts/scaffold.sh` as-is (no manual transcription), which generates the full multi-module project from the `example/` skeleton — settings + version catalog, buildSrc convention plugins, compiler-plugin with AutoService + KSP registration classes (CommandLineProcessor / CompilerPluginRegistrar, `supportsK2 = true`, no-op IR transformer, FIR registrar), KMP runtime, gradle-plugin wrapper (KotlinCompilerPluginSupportPlugin), a kctfork + Kotest unit test, and JVM/KMP integration tests using `kotlinCompilerPluginClasspath` — with every name/package/ID replaced
- Reviews the generated files (plugin ID, package, versions, replacement leftovers)
- Verifies the build (`jvmTest` + integration test runs) and optionally guides multi-Kotlin-version support strategies

## Referenced files

The prompt fetches these from GitHub instead of a local skill install:

- [skills/kotlin-compiler-plugin-setup/scripts/scaffold.sh](https://github.com/TBSten/skills/blob/main/skills/kotlin-compiler-plugin-setup/scripts/scaffold.sh) — scaffold script executed locally after a sparse clone
- [skills/kotlin-compiler-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/example) — complete project skeleton (Gradle build files + Kotlin sources with `Example` prefix)
- [skills/kotlin-compiler-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/references) — design notes for plugin registration, Gradle plugin implementation, testing patterns, publish convention, multi-version setup

## Related

- Skill version: [skills/kotlin-compiler-plugin-setup](../skills/kotlin-compiler-plugin-setup.md) — install with `gh skill install tbsten/skills kotlin-compiler-plugin-setup`
