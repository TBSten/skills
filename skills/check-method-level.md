# check-method-level

**Score how strong a testing / verification method is** on 9 axes, and report it as a Verification Score and a Level.
Puts "wrote a unit test", "looked at the screen", and "read the code and it seemed fine" on the same yardstick.

The AI only **picks a keyword for each axis**; the bundled Python script does the math.

## Install

```sh
gh skill install tbsten/skills check-method-level
```

Requirement: `python3` (3.9+, standard library only)

## When to use

- After testing / manual verification, to tell how trustworthy that verification is
- When planning how to test something and comparing candidate approaches
- When explaining why a bug slipped through (point out which axis was weak)

Adding "report the verification level with the check-method-level skill whenever you test or verify" to your CLAUDE.md makes it run every time.

## The 9 axes

| Axis | What it measures | Choices (stronger to the left) |
|---|---|---|
| Scope | How much of the system was actually exercised | e2e > integration > unit > temporary > static > none |
| Environment | How closely production conditions are reproduced | real-user > prod-like > test > mock > none |
| Execution | How automated / continuous the run is | ci > automated > manual > none |
| Evidence | What was observed from the real program | perceived > media > render-tree > structured > text > debugger > source |
| Oracle | What the result was judged against | formal > property > assertion > spec > comparison > heuristic > none |
| Observer | Who made the judgment | machine > expert > human > ai-high > ai-standard > ai-light > none |
| Coverage | How diverse the inputs / states were | exhaustive > systematic > broad > multiple > single > unknown |
| Repeatability | Whether the verification can be reproduced | full > mostly > adhoc > none |
| AI Context Quality | How much material an AI judge was given (only when Observer is an AI) | full > spec > comparison > result-only > none |

See [`check-method-level/references/rubric.md`](./check-method-level/references/rubric.md) (Japanese) for the definition of each value.

## Score formula

```text
Verification Score =
    Scope × 20 + Environment × 4 + Execution × 2 + Evidence × 3 + Oracle × 4
  + Observer × 2 + Coverage × 3 + Repeatability × 2 + AI Context Quality
```

- The rank of each value comes from its position in the strong-to-weak list; the last choice is 0 (e.g. Scope: e2e=5 … none=0)
- Scope is weighted heaviest so that the order `E2E > Integration > Unit > Temporary > Static > None` is not easily overturned by the other axes
- The reachable maximum is 197 (with Observer=machine, AI Context is 0)
- Level: S ≥160 / A ≥130 / B ≥100 / C ≥60 / D ≥20 / E >0 / - =0

### Why weights and ranks are hidden from the AI

Weights and ranks live only inside the script (`scripts/method_level_axes.py`) and never appear in SKILL.md, rubric.md, or the script output.
An AI that knows the weights tends to be generous on the heavily weighted axes and values.
This formula is documented outside the skill directory (in this document), so it is not loaded into the context when the skill runs.

## Cost (build-cost / run-cost)

When planning or comparing verification methods, report the cost next to the score (optional in post-hoc reports and miss analyses).

| Axis | What it measures | Choices (cheaper to the left) |
|---|---|---|
| build-cost | What must be newly built to run this verification | existing > add-case > new-harness > new-environment > external |
| run-cost | Time for one run (including waiting and human work) | seconds > minutes > tens-of-minutes > hours > days |

- **Why cost is not mixed into the score**: the score expresses how much confidence the verification gives. Subtracting cost would make "weak" and "strong but expensive" indistinguishable, bias choices toward cheap weak checks, and blur the cause in miss analyses
- **Why two axes**: building and running cost behave differently. A CI E2E suite is expensive to build and cheap to run; manual QA is cheap to build and expensive to run
- **Why fact-based definitions**: levels such as `low` / `medium` / `high` leave "what counts as medium" to the AI and drift. The choices are verifiable facts instead: what has to be newly prepared, and how many minutes one run takes
- When an estimate has a spread, give a range such as `--run-cost=minutes..tens-of-minutes`. Unlike the in-between values of score axes, it is not averaged; it is shown as a cheap-to-expensive range
- Give both axes together (only one exits with code 2). Money and maintenance (e.g. fixing flaky tests) are not axes; mention them in prose when they matter

```text
$ ... --build-cost=add-case --run-cost=hours..minutes
Score: 122 out of 197 (62%)
Level: B (S > A > B > C > D > E > -), Scope: unit
Axes: scope=unit env=mock ...
Cost: build=add-case run=minutes..hours
```

## Usage

```sh
python3 "${CLAUDE_SKILL_DIR}/scripts/calc-check-method-level.py" \
  --scope=unit --env=mock --execution=ci --evidence=structured --oracle=assertion \
  --observer=machine --coverage=broad --repeatability=full --label="ViewModel unit test"
```

```text
Method: ViewModel unit test
Score: 122 out of 197 (62%)
Level: B (S > A > B > C > D > E > -), Scope: unit
Axes: scope=unit env=mock execution=ci evidence=structured oracle=assertion observer=machine coverage=broad repeatability=full
Next: scope→integration, env→test, evidence→render-tree
```

- **No defaults.** If any axis is missing, the script exits with code 2 and lists the choices for the missing axes only
- Values are keywords (or aliases) only. Numbers are rejected because they are easily mistaken for score points
- **In-between values**: when a method falls between two choices, write `--scope=unit..integration`. A `Note:` line is printed
- `--ai-context` is required only when Observer is an AI (ai-high / ai-standard / ai-light)
- Inconsistent combinations print `Warn:` (e.g. an executing Scope with execution=none)
- `-v` shows the meaning of each axis value; `--format=json` prints JSON

<details>
<summary>More examples</summary>

An E2E check where an AI looks at screenshots (e.g. via Playwright) and compares them with the spec:

```text
$ ... --scope=e2e --env=staging --execution=manual --evidence=screenshot --oracle=spec \
      --observer=ai-high --ai-context=spec --coverage=multiple --repeatability=mostly
Score: 156 out of 197 (79%)
Level: A (S > A > B > C > D > E > -), Scope: e2e
Next: oracle→assertion, coverage→broad, env→prod-like
```

Reading the code and deciding "looks fine":

```text
$ ... --scope=static --env=none --execution=manual --evidence=source --oracle=heuristic \
      --observer=ai-high --ai-context=result-only --coverage=unknown --repeatability=none
Score: 33 out of 197 (17%)
Level: D (S > A > B > C > D > E > -), Scope: static
```

A screenshot test on the JVM (in-between values):

```text
$ ... --scope=unit..integration --env=emulator..mock --execution=ci --evidence=screenshot \
      --oracle=snapshot --observer=machine --coverage=multiple --repeatability=full
Score: 137 out of 197 (70%)
Level: A (S > A > B > C > D > E > -), Scope: unit..integration
Note: scope=unit..integration は選択肢の中間として扱った
Note: env=test..mock は選択肢の中間として扱った
```

</details>

## Files

```
check-method-level/
├── SKILL.md                               # Steps, cheat sheet, how to report
├── references/rubric.md                   # Definitions and examples (read only when unsure)
└── scripts/
    ├── calc-check-method-level.py         # Score calculator
    ├── method_level_axes.py               # Axes (score and cost), choices, weights, levels (SSoT)
    ├── method_level_cost.py               # Cost axes: parsing and output
    └── test_calc_check_method_level.py    # Tests
```

Tests: `python3 -m unittest discover -s skills/check-method-level/scripts`

Update the expected scores in the tests when you change the weights.
