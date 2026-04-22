# android-convention-plugins

Scalable Gradle Convention Plugins structure for Android projects.

## Features
- **Separation of Concerns**: Differentiates between functional plugins (Primitive) and structural plugins (Module).
- **Reusable DSL**: Provides extension functions to make Gradle Kotlin DSL cleaner and more type-safe.
- **Centralized Build Logic**: Move common build configuration from `build.gradle.kts` files into shared plugins.

## Prerequisites
- Android project using Gradle Kotlin DSL.
- Version Catalog (`libs.versions.toml`) for dependency management.

## How to use
1. Run `npx skills add tbsten/skills --skill android-convention-plugins`.
2. Follow `SKILL.md` to set up your `build-logic` module and migrate build logic.
