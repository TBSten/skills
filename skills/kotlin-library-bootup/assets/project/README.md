# example-lib

[![Maven Central](https://img.shields.io/maven-central/v/<group-id>/example-lib-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/<group-id>/example-lib-core)
<!-- @bootup:if ci -->
[![CI](https://github.com/<owner>/<repo>/actions/workflows/ci.yml/badge.svg)](https://github.com/<owner>/<repo>/actions/workflows/ci.yml)
<!-- @bootup:end -->
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

English |
<!-- @bootup:if !docs-site -->
[日本語](./README.ja.md)
<!-- @bootup:end -->
<!-- @bootup:if docs-site -->
[日本語](./README.ja.md) |
[Docs](https://<owner>.github.io/<repo>/) |
[API Reference](https://<owner>.github.io/<repo>/api-docs/)
<!-- @bootup:end -->

**Contents:**
[Setup](#setup) ·
[Quick Start](#quick-start) ·
[Modules](#modules) ·
<!-- @bootup:if docs-site -->
[Documentation](#documentation) ·
<!-- @bootup:end -->
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

<!-- @bootup:if kmp -->
Requires Kotlin 2.2 or later on JVM / Android.
On JS / Wasm / Native, use the same or a newer Kotlin version than this library is built with (Kotlin 2.4 or later), because Kotlin cannot read klibs from a newer compiler.
<!-- @bootup:end -->
<!-- @bootup:if jvm -->
Requires Kotlin 2.2 or later.
<!-- @bootup:end -->

<!-- @bootup:if kmp -->
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

<!-- @bootup:end -->
<!-- @bootup:if kmp -->
### JVM / Android
<!-- @bootup:end -->
<!-- @bootup:if jvm -->
### Gradle
<!-- @bootup:end -->

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

<!-- @bootup:if docs-site -->
| I want to... | API | Docs |
|---|---|---|
| TODO: describe a use case | `TODO()` | [Guide](https://<owner>.github.io/<repo>/guides/basic-usage/) |
<!-- @bootup:end -->
<!-- @bootup:if !docs-site -->
| I want to... | API |
|---|---|
| TODO: describe a use case | `TODO()` |
<!-- @bootup:end -->

## Modules

| Module | Description | Published |
|---|---|:---:|
| `example-lib-core` | The core API of example-lib | ✓ |
<!-- @bootup:if integration-test -->
| `integrationTest` | Tests example-lib through its published Maven coordinates, the way users consume it | ✗ |
<!-- @bootup:end -->
| `build-logic` | Convention plugins shared by the modules | ✗ |
<!-- @bootup:if docs-site -->
| `docs` | Documentation site (Astro Starlight) | ✗ |
<!-- @bootup:end -->

<!-- @bootup:if docs-site -->
## Documentation

- **Docs:** https://<owner>.github.io/<repo>/
- **API Reference (Dokka):** https://<owner>.github.io/<repo>/api-docs/
- **For AI agents:** https://<owner>.github.io/<repo>/llms.txt

<!-- @bootup:end -->
## Development

```bash
./gradlew build                  # Build and test everything
./gradlew jvmTest                # Fast feedback: JVM tests only
./gradlew ktlintCheck apiCheck   # Lint and binary-compatibility check (run in CI)
./gradlew apiDump ktlintFormat   # Update the API dump and format before pushing
./gradlew publishToMavenLocal    # Publish to ~/.m2 for local testing
# @bootup:if integration-test
./gradlew -p integrationTest test  # Run the integration tests
# @bootup:end
# @bootup:if docs-site
./gradlew generateApiDocs        # Generate the Dokka HTML into docs/public/api-docs
(cd docs && pnpm install && pnpm dev)  # Preview the docs site
# @bootup:end
# @bootup:if !docs-site
./gradlew generateApiDocs        # Generate the Dokka HTML into build/api-docs
# @bootup:end
```

## License

```
MIT License

Copyright (c) <year> <developer-name>
```

See [LICENSE](./LICENSE) for the full text.
