# Retrospective Meta-Analysis

Tracks which operational themes the user has had to repeat across past exploration phases, and how
many iterations passed before the rule landed in this SKILL.md. Used to decide which themes deserve
promotion to a **Core Constraint** (SKILL.md §0).

## Past-feedback table

| Theme | User repetitions | Where it landed in SKILL.md | Skill-update lag (iterations) |
|---|---:|---|---:|
| Don't stop the loop (Constraint A) | 4 | §0 Constraint A + §17 | iter 17 of 20 (85% of the phase) |
| Reflect user feedback immediately (Constraint B) | 7 | §0 Constraint B + §17 step 5 | iter 13 of 20 |
| Drive dynamic verification with MCP (PDCA) | 4 | §3 + [pdca-workflow.md](pdca-workflow.md) | iter 13 of 20 |
| Compare with sibling libraries early | 3 | §12 + [category-roles.md](category-roles.md) | iter 14 of 20 (70%) |
| Inspect CI log warnings; check API baseline | 3 | §4 / §5 | iter 8 / iter 13 |
| Allow mutation of all config files (not just build.gradle) | 1 | §3 step 2 + [pdca-workflow.md](pdca-workflow.md) | same turn |
| Parallelism causes build-tree conflict | 1 | §8 / §9 | same turn |
| Cover the library-consumer's usage pattern | 1 | (phase-specific context file) | same turn |
| Dynamic deadline change handling | 1 | §10 | same phase |
| Retrospective (KPT) must run at close-time | 1 | §17 step 5 | same phase (but post-PR initially) |

## Lag interpretation

| Lag size | Health |
|----------|--------|
| Same-turn reflection (1 repetition) | Best. Rule is recorded immediately. |
| Mid-phase (2–3 repetitions) | Acceptable but improvable. Aim for same-turn. |
| Late-phase (4+ repetitions) | Critical. The rule deserves promotion to a Core Constraint. |

Rules that needed 4+ user repetitions are surfacing **structural** failures (the rule was
violated despite being intuitively obvious). Promote them to §0 so they override section-level
detail.

## Next-phase improvement targets

- Move all lag toward "same-turn reflection". Treat any 2nd repetition as a signal that the
  reflection-to-SKILL.md flow has a friction point worth fixing.
- At phase start, re-read this file's table and pre-apply the existing Core Constraints (§0) as
  defaults.
- At close-time (§17 step 5), append any new themes that emerged in this phase to the table
  above, with the iteration count it took to land.

## How to maintain this file

After each close:

1. List the themes the user mentioned that **did not already exist** in SKILL.md
2. For each, count the repetitions across the phase
3. Add a row to the table above
4. If the count is 4+, also add the rule as a §0 Core Constraint in SKILL.md

If the count is < 4 but the rule is still operationally important, add it to the relevant section
(§1–§20) but skip §0.
