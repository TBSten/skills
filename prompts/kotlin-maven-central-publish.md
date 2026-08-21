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
- Downloads and runs [`scripts/setup-publish.sh`](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-publish.sh) via `curl`, which idempotently adds the Vanniktech Maven Publish plugin to `gradle/libs.versions.toml`, generates the buildSrc convention plugin (`publish-convention.gradle.kts`) with placeholders already filled in (GitHub URL, license, and developer info are auto-inferred from `git remote` and the `LICENSE` file), and creates the `.github/workflows/publish.yml` workflow
- Applies the convention plugin to each module to be published (agent's judgment)
- Verifies locally with `./gradlew publishToMavenLocal`
- Downloads and runs [`scripts/setup-secrets.sh`](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-secrets.sh), an interactive script covering GPG key generation, keyserver upload, private key export, and registering all 5 GitHub Secrets via `gh secret set` — the only manual step left is issuing the Sonatype Central Portal user token

## Referenced files

The prompt downloads and runs these scripts from GitHub (run as-is — never rewritten or reimplemented). The setup script fetches the `example/` templates it needs from GitHub raw by itself:

- [scripts/setup-publish.sh](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-publish.sh) — one-shot setup of catalog entry, buildSrc convention plugin, and publish workflow
- [scripts/setup-secrets.sh](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/scripts/setup-secrets.sh) — interactive GPG key + GitHub Secrets setup (supports `--dry-run`)
- [references/github-secrets.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/github-secrets.md) / [references/gpg-setup.md](https://github.com/TBSten/skills/blob/main/skills/kotlin-maven-central-publish/references/gpg-setup.md) — fallback manual instructions for environments where the scripts cannot run

## Related

- Skill version: [skills/kotlin-maven-central-publish](../skills/kotlin-maven-central-publish.md) — install with `gh skill install tbsten/skills kotlin-maven-central-publish`
