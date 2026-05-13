# PR Comment Policy

When to post a comment, how to phrase it, and how to avoid going noisy on the PR thread.

## Posting threshold

- **Only P0 / P1 findings** are candidates for PR comments.
- P2 / P3 stay in the ticket pool. They can become candidates later if dynamic evidence escalates
  the severity.

## Decision flow

1. **New finding is P0 / P1?** If no, ticket only. Stop.
2. **Same iteration already has ≥ 2 P1 findings?** Bundle them into a single "cluster" comment
   (one PR comment with multiple subsections), not separate comments.
3. **Is this a self-correction of a previous comment?** Post it as a **separate** comment — don't
   bury the correction inside an unrelated cluster. The history needs to be readable.
4. **Did a recent maintainer force-push fix this same issue?** Move the ticket to `resolved/` and
   skip the comment. Wait for the next genuinely-new P1.
5. **Already posted ≥ 1 cluster this iteration?** Additional posts are subject to saturation
   judgment — if the ROI feels marginal, skip.
6. **Cumulative comment count > 8?** Saturation warning zone. Be stricter: P1 only, and only if
   the maintainer's recent comments suggest they want it.
7. **Cumulative comment count > 12?** Stop. The thread is now noise-level for the maintainer.
8. **Maintainer latency < 1h?** Active fix phase — ROI of cluster comments is high, but noise
   risk also climbs. One concise cluster per iteration is the safe ceiling.

## Mandatory pre-post dynamic PoC

> **Any PR comment that proposes a fix must first verify the proposal with a dynamic PoC.**

Past incident: a comment proposed adding a `@get:` qualifier — verified in a later iteration to
be a Kotlin compiler syntax error across five platforms. A self-correction comment had to follow.
This rule prevents the rework.

### Pre-post PoC checklist

- [ ] Apply the proposed fix to a sandbox copy at
  `.local/tmp/exploratory-pr-<id>/iter<N>-poc/`
- [ ] Run the primary compile / test task on the most-impacted target (jvm + android + at least
  one other if it's a KMP project)
- [ ] Assert the expected behavior — public-API diff, runtime behavior, absence of new compile
  errors
- [ ] Confirm there's no surprise "fix breaks the build" outcome

If PoC passes: post the comment. If PoC fails: revise the fix and re-PoC, **or** reframe the
comment as "this is an issue, but the fix is non-trivial — here's why option A doesn't work, and
option B requires further investigation".

## Posting command

```sh
gh pr comment <NNN> --repo <owner>/<repo> --body "$(cat <<'EOF'
## Iter <N> exploration — P1 cluster (<count> issues)

### #0042 — <ticket title>
**Location**: `path/to/file.kt:42`
**Severity**: P1
<2–3 sentence description>
**Fix proposal**: <one-line summary + link to ticket file>

### #0043 — <ticket title>
...
EOF
)"
```

## Anti-noise hygiene

- Read existing comments before posting: `gh pr view <NNN> --json comments --jq '.comments[].body'`
- Don't repeat: if your finding is already in a maintainer comment, drop the candidate
- One cluster per iteration unless something genuinely new lands mid-iteration (e.g. force-push
  introduces a regression)
- Don't post sub-tickets that depend on an earlier ticket; consolidate them into the parent's
  comment

## Self-correction comment format

If a previous comment was wrong:

```markdown
## Self-correction for [comment URL]

The fix I proposed in iter <N> doesn't work. Verified in iter <M> via PoC:

<reproduction of the failure>

Updated proposal:
<new fix, ideally also PoC-verified>

Apologies for the noise.
```

Post this as its **own** comment, not inside a cluster. The maintainer needs to see the trail.

## Latency-aware cadence

Cross-reference SKILL.md §16 — maintainer-response latency dictates the posting cadence:

- **< 1h** (active): one cluster per iteration is OK; consider mid-iteration follow-up only for
  P0
- **1–3h** (normal): one cluster per iteration; no mid-iteration follow-up
- **3–9h** (busy): bias toward holding the comment; consolidate into the next iteration
- **> 9h** (quiet): only post if P0 or if maintainer engagement signal returns

When latency drops sharply (e.g. 9h → 40min), the maintainer has entered an active phase — your
next 1–2 iterations are the last opportunity for productive cluster comments before the PR
deadline.
