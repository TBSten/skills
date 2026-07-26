# intellij-plugin-dev

A tooling & workflow reference for developing IntelliJ Platform / Android Studio plugins with minimal human effort, agent-driven. It captures the knowledge distilled from building a plugin with a Jewel/Compose tool window + a Kotlin Analysis API frontend + gutter line markers.

## Core idea

IDE plugins make it unclear whether you have verification channels beyond "install into the IDE and click around by hand" — so the first risk is whether you can form a feedback loop at all. This skill decomposes verification into **6 channels** and anchors on two of them:

- **Correctness = headless functional tests** (`BasePlatformTestCase` + Kotlin Analysis API)
- **Appearance = headless PNG self-review** (`renderComposeScene` + standalone Jewel)

Real-IDE channels (Driver / physical Android Studio) are pushed to periodic checkpoints rather than every iteration. This lets an agent run most of implement → verify → fix without a human in the loop.

## What it does

1. **Wire the build** — `intellijPlatform` (v2) / bundled Kotlin (AA), Jewel, Compose, Skiko / JBR21 / K2 / since-until, up to passing `compileKotlin`, `buildPlugin`, `runIde`
2. **Write functional tests** — Test an annotation-analyzing frontend (Analysis API / K2) headlessly on `BasePlatformTestCase`: resolving generic-annotation type arguments, annotation-stub fixtures, and suppressing unrelated logged errors
3. **Iterate appearance headlessly** — Bake Jewel/Compose UI to PNG with `renderComposeScene` and gate regressions with a VRT golden (transparent-corner check, managed clean, machine-decidable gates)
4. **Integrate into the IDE** — Wire the tool window (`addComposeTab`), gutter line markers, editor following, node→source navigation, and PSI insertion (code generation), while avoiding the lifecycle and performance traps
5. **Add Driver smoke** — Wire a thin real-IDE E2E smoke as a periodic checkpoint, and inspect the live UI tree (locators)

## When to use

- Starting a new IntelliJ / Android Studio plugin
- Implementing a Jewel/Compose tool window or gutter line markers
- Stuck on "the Analysis API won't run in tests" or "I can't get the annotation's type argument"
- Verifying UI appearance without launching the IDE / setting up a VRT golden
- Wiring editor following, node→source navigation, or PSI insertion (code generation)
- Adding a real-IDE Driver smoke test
- Hitting platform-specific traps such as the Android Studio vs. IntelliJ build-number skew

## How it works

1. **Step 1**: Use SKILL.md's "core idea" to decompose verification into 6 channels and confirm the two-anchor plan (functional tests / PNG review)
2. **Step 2**: Wire the build via `references/setup/` (basics → preview → snapshot)
3. **Step 3**: Open the usage reference for what you're implementing (functional tests = `analysis-api-testing.md` / appearance = `headless-preview.md` / IDE integration = `ide-integration.md`)
4. **Step 4**: For interaction/timing that headless can't cover, place the Driver smoke from `driver-smoke.md` at a periodic checkpoint
5. **Step 5**: On Compose Desktop / IntelliJ-specific traps, jump from the index in `gotchas.md` to its primary entry

## Bundled references

| File | Description |
|---|---|
| `references/setup/basics.md` | Base build wiring (intellijPlatform / SDK 261 / bundled Kotlin(AA), Jewel, Compose, Skiko / JBR21 / K2 / since-until). Unified-distribution trap; not bundling stdlib |
| `references/setup/preview.md` | Build wiring to bake preview PNGs (shared source set / standalone Jewel & Compose Desktop / `:icons` / `updatePreview`·`verifyPreview` tasks) |
| `references/setup/snapshot.md` | VRT golden wiring (`snapshots/preview` location / update=sync·verify=compare / alpha=255·managed-clean gates / CI gate) |
| `references/analysis-api-testing.md` | Functional tests that analyze annotations via the AA. Resolving generic-annotation type args / annotation-stub fixtures / suppressing unrelated logged errors / resilience to broken code |
| `references/headless-preview.md` | `renderComposeScene` + standalone Jewel + VRT golden. Recommended workflow (baseline → verify → review → human sign-off) and automated gates |
| `references/ide-integration.md` | tool window / `addComposeTab` / gutter line markers / editor following / navigation / PSI insertion / lifecycle (stale race, dumb mode) / performance (backgrounding, cancellation, the `runCatching` trap) |
| `references/driver-smoke.md` | Real-IDE Driver smoke. Two-layer setup (thick functional tests + thin Driver) / inspecting the UI tree (locators) / internal debug AnActions |
| `references/gotchas.md` | Compose Desktop / IntelliJ-specific traps (pinch not delivered, AS vs IDEA build skew, rejected rendering approaches) and an index to each trap |

## Prerequisites

- A Kotlin project and the IntelliJ Platform Gradle Plugin (v2)
- Target IDE: verified against IntelliJ Platform 2026.1 (build 261); adjust versions to your own target IDE
- For Jewel/Compose UI: JBR 21 / Compose Desktop / Skiko / standalone Jewel
- For the Analysis API: K2 enabled (`idea.kotlin.plugin.use.k2` + `supportsKotlinPluginMode supportsK2="true"`)
- Optional: JetBrains MCP (useful for fact-checking via `get_file_problems`, etc.)

## Install

```sh
gh skill install tbsten/skills intellij-plugin-dev
```
