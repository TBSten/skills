# Ticket Bookkeeping

Each finding becomes a Markdown ticket under `.local/tmp/exploratory-pr-<id>/problems/`. The
orchestrator and all five cats follow the rules here to keep numbering collision-free, prevent
duplicates, and let the close-time cluster analysis (see [cluster-families.md](cluster-families.md))
run smoothly.

## Numbering

- Four-digit prefix (`#0001`, `#0102`, …)
- **Numbers are allocated only by `scripts/new-ticket.sh`.** It scans `problems/` — including
  `resolved/` / `methodology/` / `non-pr/`, so moved tickets keep their numbers reserved — and
  claims the next number atomically. Five parallel cats can create tickets simultaneously with
  no collisions, no per-cat reservation ranges, and no renumbering.
- Never hand-write a ticket file or invent a number. Create, then fill in the body:

```sh
"$SKILL_ROOT/scripts/new-ticket.sh" --id <pr-id> --cat 3 --iter 7 --severity P2 --slug gradle-cache-race
# → prints .local/tmp/exploratory-pr-<id>/problems/NNNN-gradle-cache-race.md
```

The script prefills the H1, `## Severity`, and `## Owner`; the cat fills in `## Location` /
`## Detail` / `## Fix proposal` (and `## Dynamic evidence` where relevant).

## Directory layout

```
.local/tmp/exploratory-pr-<id>/problems/
├── 0001-<slug>.md              # active
├── 0002-<slug>.md
├── ...
├── resolved/                    # maintainer fixed the underlying issue
│   └── 0007-<slug>.md
├── methodology/                 # process / orchestrator / exploration meta
│   └── 0042-<slug>.md
└── non-pr/                      # underlying root cause is outside the PR
    └── 0103-<slug>.md
```

- **active**: still relevant; candidate for PR comment
- **resolved/**: maintainer pushed a fix that closes it; keep for traceability
- **methodology/**: rules about exploration itself (e.g. "parallel cats stomped on each other's
  `build/`"). Feed these back into SKILL.md, not into a PR comment.
- **non-pr/**: caused by external state (upstream lib bug, CI infra flake). Document but don't
  surface on the PR — open a separate issue if warranted.

## File format

```markdown
# NNNN. <Short title>

## Severity

P0 / P1 / P2 / P3

## Owner

cat1 (iter 14) — source-code static analysis

## Location

- `path/to/file.kt:line`

## Detail

(Reproduction steps / evidence / why this matters)

## Fix proposal

Option (a): …
Option (b): …
```

### Severity guidelines

| Severity | Definition |
|----------|------------|
| P0 | Library consumer will hit a build failure / runtime crash on a default path |
| P1 | Public API behavior is broken, or a security / data-integrity concern, or a critical docs error |
| P2 | Internal inconsistency, refactor opportunity, future-brittleness signal |
| P3 | Minor noise, typo, optional improvement |

P0/P1 → candidate for PR comment (see [pr-comment-policy.md](pr-comment-policy.md)).
P2/P3 → ticket only.

### Severity re-evaluation cycle

Severity is **not frozen at issuance time**. Re-evaluate when:

- Dynamic verification adds / removes evidence (e.g. cat3 finds the "P1 bug" doesn't reproduce
  on jvm — drop to P2 with a `## Dynamic evidence` section)
- Sibling-library comparison reveals it's an industry-norm limitation (often P1 → P3)
- Maintainer reacts in a way that confirms / disputes the severity

## Deduplication

- Before writing, check `ls problems/` for an existing slug that matches
- If the same phenomenon is already reported, **add to it** rather than creating a duplicate.
  Consider clustering it into an existing ticket family.
- Severity labels are reassessed at each iteration's end (and during the close-time cluster
  analysis) — do not lock them at issuance time.

## FINAL-SUMMARY linkage

At close time, FINAL-SUMMARY.md cites tickets by number (`#0042`) and groups them by family
([cluster-families.md](cluster-families.md)). Keep ticket titles concise enough to appear in a
table row.
