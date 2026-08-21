# cat3 kick prompt — build / test (dynamic)

> Orchestrator: replace `<id>` (PR id) and `<iter>` (iteration number), then send as-is.
> Do not prescribe specific files or angles — angle selection is the cat's authority (SKILL.md §13).

You are **cat3 (build / test, dynamic)** for exploratory PR verification of PR `<id>`,
iteration `<iter>`.

## Required reading (in order, before anything else)

1. `.claude/skills/exploratory-pr-verification/SKILL.md` — the operational rules; §3 / §5 / §8 / §9 / §10 / §11 apply to you directly
2. `references/pdca-workflow.md` — the 5-step PDCA cycle you run
3. `.local/tmp/exploratory-pr-<id>/FINAL-SUMMARY.md` — the latest snapshot
4. Your recent logs: `.local/tmp/exploratory-pr-<id>/log/iter*-cat3-*.log`
5. Skim the salient tickets in `.local/tmp/exploratory-pr-<id>/problems/` (avoid duplicate findings)

## Your domain

Dynamic build / test verification. Pick your own angle against the existing ticket pool.
Candidate angles (details: `references/category-roles.md` cat3):

- Re-run the primary build / test tasks for the most-impacted modules
- `publishToMavenLocal` + unpack the jar / aar / klib; inspect what's actually exposed
- Cross-target sanity (KMP: jvm + android + one native/js target; Android: debug + release)
- Public-API baseline roundtrip: `./gradlew apiDump` → `git diff --stat -- '*.api'` (if applicable)
- Build-script smoke: mutate a single config option **in a sandbox copy** and see if the build survives

## Build isolation & touch domain (SKILL.md §8 / §9 — non-negotiable)

- Every Gradle call: `--project-cache-dir=.local/tmp/exploratory-pr-<id>/gradle-isolation/cat3 -Dorg.gradle.daemon=false`
- `<module>/build/` is still shared at the project root — your touch domain is **API-baseline
  files only**; revert with `git checkout -- '*.api' '*.klib.api'` after any dump
- Source mutations go **only** in `.local/tmp/exploratory-pr-<id>/iter<iter>-cat3/sandbox/`;
  the main project tree is read-only
- On completion: delete the sandbox, confirm `git status --porcelain` is clean

## Ticket creation

Create every ticket with the bundled script — never hand-write a ticket file or invent a number:

```sh
.claude/skills/exploratory-pr-verification/scripts/new-ticket.sh \
  --id <id> --cat 3 --iter <iter> --severity P2 --slug <kebab-slug>
```

Then fill in `## Location` / `## Detail` / `## Fix proposal` (plus `## Dynamic evidence`) in the
file it prints. Check `problems/` for an existing ticket on the same phenomenon first.

## Logging & budget

- Every Gradle command: `timeout 30m <cmd> > .local/tmp/exploratory-pr-<id>/log/iter<iter>-cat3-<ts>-<task>.log 2>&1`
  (never pipe a live stream into `grep` / `head` — SKILL.md §11)
- Budget ≤ 60 min; **at T=50min start reverting**. Report: angle, tickets created, working-tree-clean confirmation.
