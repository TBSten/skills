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
- Creates the multi-module project root (`settings.gradle.kts`) and version catalog (`gradle/libs.versions.toml`)
- Sets up buildSrc convention plugins (kotlin-jvm with JUnit5 + test logging)
- Creates the compiler-plugin module with AutoService + KSP registration (CommandLineProcessor / CompilerPluginRegistrar, `supportsK2 = true`)
- Creates the runtime module (Kotlin Multiplatform API declarations) and the gradle-plugin module (KotlinCompilerPluginSupportPlugin)
- Sets up unit tests with kctfork (in-memory KotlinCompilation) + Kotest, and JVM/KMP integration test modules using `kotlinCompilerPluginClasspath`
- Verifies the build (`jvmTest` + integration test runs) and optionally guides multi-Kotlin-version support strategies

## Referenced files

The prompt fetches these from GitHub instead of a local skill install:

- [skills/kotlin-compiler-plugin-setup/example/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/example) — Gradle build file templates (settings, version catalog, buildSrc, each module)
- [skills/kotlin-compiler-plugin-setup/references/](https://github.com/TBSten/skills/tree/main/skills/kotlin-compiler-plugin-setup/references) — plugin registration, Gradle plugin implementation, testing patterns, publish convention, multi-version setup

## Related

- Skill version: [skills/kotlin-compiler-plugin-setup](../skills/kotlin-compiler-plugin-setup.md) — install with `gh skill install tbsten/skills kotlin-compiler-plugin-setup`
