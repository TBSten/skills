# PDCA Workflow for Dynamic Verification (cat3 / cat4 / cat5)

A 5-step cycle for any dynamic verification work. The steps mirror Plan-Do-Check-Act, with an
optional loop-back.

## 1. Environment setup

- Create an isolated Gradle project-cache:
  `--project-cache-dir=.local/tmp/exploratory-pr-<id>/gradle-isolation/cat<N>`
- Disable the Gradle daemon: `-Dorg.gradle.daemon=false`
- If artifact-level verification is needed: `./gradlew publishToMavenLocal` first, then point a
  sandbox consumer at `mavenLocal()`
- Confirm the relevant version-catalog entries (`libs.versions.toml` if present)

## 2. Config mutation

**Any** of these are fair game for mutation in a sandbox:

- Module `build.gradle.kts` (each module + root)
- `settings.gradle.kts` (composite build / module composition / `includeBuild`)
- `gradle.properties` (Kotlin / Android settings, `org.gradle.jvmargs`, `kotlin.code.style`, …)
- `libs.versions.toml` (version catalog, Kotlin / AGP / Compose version switches)
- Convention-plugin sources in `buildLogic/` or `build-logic/` (if the project uses them)
- API-validation config block (when applicable: `apiValidation { ignoredPackages = … }`)
- `local.properties` (SDK path, etc.)
- `consumer-rules.pro` / `proguard-rules.pro` (Android)
- `.kotlin-version` / `.tool-versions`
- Source code (test fixture / sample) — **sandbox only**; the main project tree is read-only

The rule: anything you can hot-edit in a sandbox, you can mutate. Just confine the mutation to
`.local/tmp/exploratory-pr-<id>/iter<N>-cat<X>/sandbox/` and revert on completion.

## 3. Actually run (MCP required for cat4)

Pick the runner that matches the target surface:

### Compose Multiplatform / KMP

| Target | Launch command | MCP driver |
|--------|---------------|------------|
| JS / Wasm | `./gradlew :dev:jsBrowserDevelopmentRun` | `mcp__playwright__*` |
| Android | `./gradlew :dev:installDebug` then launch via adb | `mcp__maestro__*` or `mcp__mobile-mcp__*` |
| Desktop | `./gradlew :dev:run` | screenshot tooling, `mcp__claude-in-chrome__*` for embedded views |
| iOS | `./gradlew :dev:iosSimulatorArm64Test` or open the Xcode workspace | `mcp__maestro__*` |

### Android-only library

- `./gradlew :sample:installDebug` then `mcp__maestro__launch_app` / `mcp__mobile-mcp__*`

### JVM library / CLI

- `./gradlew :sample:run -PsomeArg=...`
- For long-running services: spawn with `run_in_background` and tail the log file

### IDE plugin

- `mcp__jetbrains__execute_run_configuration`

### Server-side Kotlin

- `./gradlew :server:run` (Ktor / Spring Boot etc.), then `curl` / `mcp__playwright__*` against
  the hosted endpoint

## 4. Confirm output (MCP-driven for cat4)

| Surface | Verification |
|---------|--------------|
| Web UI | `mcp__playwright__browser_snapshot` / `browser_console_messages` / `browser_evaluate` |
| Native app | `mcp__maestro__inspect_view_hierarchy` / `take_screenshot` |
| Artifact (jar / aar / klib) | `unzip -p <artifact> META-INF/...` / `javap -v <class>` |
| Generated code | `find build/generated -name "*.kt" -exec wc -l {} +` then `Read` |
| API baseline | `git diff --stat -- '*.api' '*.klib.api'` |
| CI log | `gh run view <run-id> --log > <log-file>` then grep |

## 5. (Optional) Loop steps 1–3 with varied options

If step 4 surfaces an unexpected result, mutate one variable in step 2 and re-run:

- Toggle an experimental compiler option
- Switch a version-catalog entry (Kotlin 2.x → 2.x+1, AGP, Compose)
- Vary the option / scope / preview count exposed by the feature
- Cross-target: re-run on the next-most-impacted target

Cap the loop at the cat's 60-min budget (SKILL.md §10). Document every loop variant in the
ticket's `## Dynamic evidence` section.

## Output preservation

Every command in this PDCA cycle must write to a log file:

```sh
./gradlew <task> --continue \
  --project-cache-dir=.local/tmp/exploratory-pr-<id>/gradle-isolation/cat<N> \
  -Dorg.gradle.daemon=false \
  > .local/tmp/exploratory-pr-<id>/iter<N>-cat<X>-<ts>-<task>.log 2>&1
```

No piping into `grep` / `head` / `tail` on the live stream — see SKILL.md §11 "Log preservation
rule". Filter against the saved file instead.

## Domain conflict avoidance

`<module>/build/` is shared at the project root even with `--project-cache-dir` isolation.
Parallel cats touching the same module's `build/` will race. The SKILL.md §9 split-by-domain
rule is the mitigation:

- cat1 / cat2: read-only — no build at all
- cat3: API-baseline files (revert with `git checkout -- '*.api' '*.klib.api'`)
- cat4: sample-app / integration-test directory (revert)
- cat5: cat5-owned stress directory only

If cat3 and cat4 both need the same module's `build/`, kick them sequentially within that
iteration (the orchestrator decides).
