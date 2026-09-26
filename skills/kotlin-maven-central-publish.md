# kotlin-maven-central-publish

Set up Maven Central publishing for Kotlin/KMP projects using Vanniktech Maven Publish plugin, GPG signing, and GitHub Actions CI/CD.

## Install

```sh
gh skill install tbsten/skills kotlin-maven-central-publish
```

## Overview

This skill automates the setup of Maven Central publishing for Kotlin and Kotlin Multiplatform projects. The mechanical setup is done by two bundled scripts, which the agent runs as-is (no reading, rewriting, or reimplementing):

- **`scripts/setup-publish.sh`** — idempotently adds the Vanniktech Maven Publish plugin (version auto-selected from the Gradle wrapper / Kotlin version) and your library version to the version catalog, generates the convention plugin (`publish-convention.gradle.kts`) in `buildSrc` or an included build such as `build-logic` with placeholders already filled in (GitHub URL, license, and developer info are auto-inferred from `git remote` and the `LICENSE` file), and creates the GitHub Actions workflows (runner auto-selected by whether Apple targets exist). It also reports plugin requests that must be rewritten for the chosen layout. Safe to re-run; never overwrites existing files without `--force`; emits a one-line result JSON.
- **`scripts/setup-secrets.sh`** — interactive script that generates a GPG key, sends the public key to keyservers, exports the private key, and registers all 5 GitHub Secrets via `gh secret set`. The only remaining manual step is issuing the Sonatype Central Portal user token. Supports `--dry-run`.

## What Gets Generated

| File | Description |
|---|---|
| `<convention-dir>/src/main/kotlin/publish-convention.gradle.kts` | Convention plugin with Central Portal config, conditional signing, flat artifactId, and POM metadata (`<convention-dir>` = `buildSrc` by default, or e.g. `build-logic`) |
| `<convention-dir>/build.gradle.kts` | Puts Vanniktech Maven Publish and the Kotlin (/ Android) Gradle plugin on the convention build's classpath |
| `<convention-dir>/settings.gradle.kts` | Imports the root version catalog and declares repositories |
| `gradle/libs.versions.toml` | Updated with the Maven Publish plugin and (optionally) your library version |
| `.github/workflows/publish.yml` | Release-driven publishing with tag/version and non-SNAPSHOT checks, dry-run / stage-only manual runs |
| `.github/workflows/publish-check.yml` | Runs `publishToMavenLocal` on every PR to catch broken publishing config early |

## Prerequisites

- Kotlin project with Gradle and version catalog
- GitHub repository
- Sonatype Central Portal account with verified namespace
- GPG key for artifact signing (`scripts/setup-secrets.sh` can generate one)

## Usage

After installation, invoke with:
- "Maven Central に公開したい"
- "Set up Maven Central publishing"
- "publishToMavenLocal できるようにして"

The skill collects project information, runs `scripts/setup-publish.sh` to generate the configuration files, applies the convention plugin to the modules to publish, and runs `scripts/setup-secrets.sh` to set up GPG keys and GitHub Secrets. `references/gpg-setup.md` and `references/github-secrets.md` serve as fallback manual instructions for environments where the scripts cannot run.

Further references:

- `references/convention-options.md` — buildSrc vs build-logic, signing strategies, artifactId / coordinates, version SSoT (catalog vs `VERSION_NAME`, no-SNAPSHOT policy), Dokka javadoc, `languageVersion` / `apiVersion` floor for older Kotlin consumers
- `references/release-flow.md` — why the workflow looks the way it does, and the tag-push + auto GitHub Release alternative
- `references/troubleshooting.md` — common errors (classpath conflicts, `SonatypeHost` removal, `is final`, 401, `BAD_PASSPHRASE`, ...)

## Key Technical Details

- Uses **Vanniktech Maven Publish** plugin (0.37.0 by default; 0.35.0 / 0.34.0 for older Gradle / Kotlin) — `publishToMavenCentral()` without `SonatypeHost` (removed in 0.34.0)
- Targets **Sonatype Central Portal** (not legacy OSSRH)
- GPG signing is **conditional** — skipped only for `*ToMavenLocal` tasks (judged by the last path segment), so CI fails fast when secrets are missing
- artifactId is derived from `project.path` (`:ksp:processor` → `ksp-processor`); modules override only the POM name / description
- The Kotlin Gradle plugin shares the convention build's classpath with Vanniktech (required since 0.36)
- Publishes via `publishAndReleaseToMavenCentral` task with `--no-configuration-cache`

## Required GitHub Secrets

| Secret | Description |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal user token username |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal user token password |
| `SIGNING_KEY_ID` | GPG key short ID (last 8 hex digits of fingerprint) |
| `SIGNING_PASSWORD` | GPG key passphrase |
| `GPG_KEY_CONTENTS` | GPG private key in ASCII armor format |
