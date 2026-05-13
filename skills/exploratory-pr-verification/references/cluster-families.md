# Cumulative Ticket Family Clusters

At close time, every ticket is mapped to one of these families. FINAL-SUMMARY.md groups tickets
by family so the follow-up PR scope can be designed coherently (one family per follow-up PR is
the default heuristic).

## C-1: docs gap

README / docs site / KDoc primary example drifted from the source's current shape. The "what's
new" of the PR isn't reflected in user-facing documentation.

Typical fix: docs-only PR, no code change.

## C-2: SoT (Single Source of Truth) violation

The same constant / config / type definition is hardcoded in multiple places. Refactor to extract
a shared constant / utility.

Typical fix: small refactor PR. Often catches a future-brittleness bug at the same time.

## C-3: silent failure

Errors / warnings are swallowed without surfacing — empty list / null / fallback paths return
without telemetry. The library consumer cannot tell whether the operation worked.

Typical fix: surface via `error()` / `warning()` / log, or change the public type to express the
failure (`Result<T>`, sealed class).

## C-4: public-API / ABI break

`*.api` / `*.klib.api` baseline does not match the implementation. May indicate:

- A missing `apiDump` after a public-API change
- An `ignoredPackages` / `nonPublicMarkers` config gap that lets internal types leak
- An unintentional binary-incompatible change

Typical fix: re-run apiDump, audit `ignoredPackages` config, decide whether the API change is
intentional.

## C-5: name discovery / cross-module

Synthetic / generated declaration name collision between modules, or a downstream consumer
cannot find a generated symbol because the discovery mechanism doesn't span module boundaries.

Common in Kotlin Compiler Plugin / annotation-processor projects.

Typical fix: improve name uniqueness (hash module name into the synthetic name) or document the
cross-module limitation.

## C-6: test brittleness

Test fixture hardcodes a hash / FQN / platform-specific path. The test passes on one platform
and fails on another, or breaks when an unrelated module is renamed.

Typical fix: replace hardcoded values with computed ones, add a tolerance band, or split the
test by platform.

## C-7: publish / supply chain

Maven coordinates, artifact metadata, POM file, or reproducible-build invariant is off.
Includes missing source/jar publication, wrong group ID, license metadata gaps.

Typical fix: publish-flow PR. Test by `publishToMavenLocal` and `unzip` the artifact.

## C-8: sample-app / dogfood gap

The project's demo / dev module does not reproduce the latest behavior of the main library. Often
the demo lags behind a public-API change.

Typical fix: update the demo to use the new public API.

## C-9: process / methodology

The finding is about the exploration process itself — parallel cats stomped on each other's
`build/`, the orchestrator failed to detect a force-push, etc. These don't go on the PR.

Typical fix: update SKILL.md or `references/*.md`. File the ticket under `methodology/`.

## C-10: upstream-library limitation

The phenomenon is caused by a bug or design choice in a dependency (the binary-compatibility
validator, the Kotlin compiler, the IDE plugin, the build tool). No workaround in the PR's
project.

Typical fix: file an upstream issue, link from a `non-pr/` ticket, document the workaround if
any.

## C-11: IDE-vs-CLI asymmetry

IntelliJ K2 plugin and the CLI compiler disagree — error reported in one but not the other, or
reported differently. Confuses contributors.

Typical fix: file an upstream issue against the IDE plugin or the compiler; document the
workaround.

## Adding a new family

If a finding doesn't fit any family, name a new one (C-12 …) and add the row here. Keep families
broad enough that 3+ tickets can plausibly belong; if it's a one-off, attribute it to the closest
existing family instead.

## FINAL-SUMMARY.md template snippet

```markdown
## Ticket clusters (close-time snapshot)

| Family | Active count | Tickets | Follow-up PR scope |
|--------|--------------|---------|--------------------|
| C-1 docs gap | 3 | #0042, #0051, #0073 | docs-only PR covering all three |
| C-2 SoT | 2 | #0046, #0058 | small refactor PR |
| C-3 silent failure | 1 | #0061 | bundle with C-2 if scope allows |
| C-4 ABI | 0 | (resolved) | — |
| C-5 name discovery | 1 | #0079 | upstream-blocked; track separately |
| C-9 methodology | 4 | (under `methodology/`) | reflected into SKILL.md |
```
