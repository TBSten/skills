# example-lib

[![Maven Central](https://img.shields.io/maven-central/v/<group-id>/example-lib-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/<group-id>/example-lib-core)
[![CI](https://github.com/<owner>/<repo>/actions/workflows/ci.yml/badge.svg)](https://github.com/<owner>/<repo>/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

English |
[日本語](./README.ja.md) |
[Docs](https://<owner>.github.io/<repo>/) |
[API Reference](https://<owner>.github.io/<repo>/api-docs/)

**Contents:**
[Setup](#setup) ·
[Quick Start](#quick-start) ·
[Modules](#modules) ·
[Documentation](#documentation) ·
[Development](#development) ·
[License](#license)

---

<description>

> [!IMPORTANT]
> example-lib is experimental. Until it reaches 1.0.0, minor releases may contain breaking changes.

## Setup

|                          |                                                                                                                  |
|--------------------------|------------------------------------------------------------------------------------------------------------------|
| `<example-lib-version>` | ![Maven Central](https://img.shields.io/maven-central/v/<group-id>/example-lib-core.svg?label=%20) |

Requires Kotlin 2.2 or later.

### Kotlin Multiplatform

```kts
// module/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("<group-id>:example-lib-core:<example-lib-version>")
        }
    }
}
```

### JVM / Android

```kts
// module/build.gradle.kts
dependencies {
    implementation("<group-id>:example-lib-core:<example-lib-version>")
}
```

### Version catalog

```toml
# gradle/libs.versions.toml
[versions]
example-lib = "<example-lib-version>"

[libraries]
example-lib-core = { module = "<group-id>:example-lib-core", version.ref = "example-lib" }
```

## Quick Start

```kt
import com.example.kotlinlibrarybootup.*

// TODO: Show the smallest useful example of example-lib here.
```

| I want to... | API | Docs |
|---|---|---|
| TODO: describe a use case | `TODO()` | [Guide](https://<owner>.github.io/<repo>/guides/basic-usage/) |

## Modules

| Module | Description | Published |
|---|---|:---:|
| `example-lib-core` | The core API of example-lib | ✓ |
| `integrationTest` | Tests example-lib through its published Maven coordinates, the way users consume it | ✗ |
| `build-logic` | Convention plugins shared by the modules | ✗ |
| `docs` | Documentation site (Astro Starlight) | ✗ |

## Documentation

- **Docs:** https://<owner>.github.io/<repo>/
- **API Reference (Dokka):** https://<owner>.github.io/<repo>/api-docs/
- **For AI agents:** https://<owner>.github.io/<repo>/llms.txt

## Development

```bash
./gradlew build                  # Build and test everything
./gradlew jvmTest                # Fast feedback: JVM tests only
./gradlew ktlintCheck apiCheck   # Lint and binary-compatibility check (run in CI)
./gradlew apiDump ktlintFormat   # Update the API dump and format before pushing
./gradlew publishToMavenLocal    # Publish to ~/.m2 for local testing
./gradlew -p integrationTest test  # Run the integration tests
./gradlew generateApiDocs        # Generate the Dokka HTML into docs/public/api-docs
(cd docs && pnpm install && pnpm dev)  # Preview the docs site
```

## License

```
MIT License

Copyright (c) <year> <developer-name>
```

See [LICENSE](./LICENSE) for the full text.
