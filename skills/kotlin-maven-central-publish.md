# kotlin-maven-central-publish

Set up Maven Central publishing for Kotlin/KMP projects using Vanniktech Maven Publish plugin, GPG signing, and GitHub Actions CI/CD.

## Install

```sh
npx skills add tbsten/skills \
  --skill kotlin-maven-central-publish
```

## Overview

This skill automates the setup of Maven Central publishing for Kotlin and Kotlin Multiplatform projects. It creates:

- A **buildSrc convention plugin** (`publish-convention.gradle.kts`) using the Vanniktech Maven Publish plugin
- A **GitHub Actions workflow** (`publish.yml`) triggered on GitHub Releases
- Complete **POM metadata** (license, developer info, SCM)
- **Conditional GPG signing** (active only when keys are available)

## What Gets Generated

| File | Description |
|---|---|
| `buildSrc/src/main/kotlin/publish-convention.gradle.kts` | Convention plugin with Sonatype Central Portal config, signing, and POM metadata |
| `buildSrc/build.gradle.kts` | Updated with Vanniktech Maven Publish dependency |
| `gradle/libs.versions.toml` | Updated with Maven Publish plugin version |
| `.github/workflows/publish.yml` | GitHub Actions workflow for automated publishing |

## Prerequisites

- Kotlin project with Gradle and version catalog
- GitHub repository
- Sonatype Central Portal account with verified namespace
- GPG key for artifact signing

## Usage

After installation, invoke with:
- "Maven Central に公開したい"
- "Set up Maven Central publishing"
- "publishToMavenLocal できるようにして"

The skill will guide you through collecting project information, generating configuration files, and providing manual setup instructions for secrets and credentials.

## Key Technical Details

- Uses **Vanniktech Maven Publish** plugin (v0.30.0+) for simplified Maven Central integration
- Targets **Sonatype Central Portal** (not legacy OSSRH)
- GPG signing is **conditional** — skipped during local development, active in CI when secrets are provided
- The `libs.plugins.mavenPublish.map { ... }` pattern converts a plugin ID to a dependency coordinate for buildSrc usage
- Publishes via `publishAndReleaseToMavenCentral` task with `--no-configuration-cache`

## Required GitHub Secrets

| Secret | Description |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal user token username |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal user token password |
| `SIGNING_KEY_ID` | GPG key short ID (last 8 hex digits of fingerprint) |
| `SIGNING_PASSWORD` | GPG key passphrase |
| `GPG_KEY_CONTENTS` | GPG private key in ASCII armor format |
