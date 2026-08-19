---
status: Experimental
group: Kotlin ライブラリ/ツール開発
---

# kotlin-maven-central-publish Prompt

[日本語](./kotlin-maven-central-publish.ja.md) | [DeepWiki](https://deepwiki.com/TBSten/skills)

One-shot prompt version of the [kotlin-maven-central-publish skill](../skills/kotlin-maven-central-publish.md). Sets up Maven Central publishing for Kotlin/KMP projects — a buildSrc convention plugin using Vanniktech Maven Publish, GPG signing, and a GitHub Actions CI/CD workflow targeting Sonatype Central Portal — without installing the skill.

## Run

Paste the following into Claude Code (or any coding agent):

```
Fetch https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/prompts/kotlin-maven-central-publish/PROMPT.md and follow its instructions
```

## What it does

- Collects project info (group ID, version, license, GitHub URL, developer info, target modules)
- Adds the Vanniktech Maven Publish plugin to `gradle/libs.versions.toml`
- Creates a buildSrc convention plugin (`publish-convention.gradle.kts`) with Sonatype Central Portal config, conditional GPG signing, and POM metadata
- Applies the convention plugin to each module to be published
- Creates a `.github/workflows/publish.yml` workflow triggered on GitHub Releases (with `workflow_dispatch` support)
- Verifies locally with `./gradlew publishToMavenLocal`
- Guides you through manual setup: GPG key generation, Sonatype Central Portal account, and the 5 required GitHub Secrets

## Referenced files

The prompt fetches these from GitHub instead of a local skill install:

- [example/buildSrc-build.gradle.kts](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/example/buildSrc-build.gradle.kts) — buildSrc build script with the Vanniktech Maven Publish dependency
- [example/publish-convention.gradle.kts](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/example/publish-convention.gradle.kts) — convention plugin template with placeholders
- [example/publish.yml](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/example/publish.yml) — GitHub Actions publish workflow template
- [references/github-secrets.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/github-secrets.md) — required GitHub Secrets and how to obtain them
- [references/gpg-setup.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/gpg-setup.md) — GPG key generation and export instructions

## Related

- Skill version: [skills/kotlin-maven-central-publish](../skills/kotlin-maven-central-publish.md) — install with `gh skill install tbsten/skills kotlin-maven-central-publish`
