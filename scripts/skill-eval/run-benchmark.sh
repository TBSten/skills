#!/usr/bin/env bash
# Benchmark: skill の evals/<case>/ を `claude plugin eval` で skill あり / なしの 2 本立てで実行し、要約を JSON にする。
# 実際にモデルを呼ぶ (課金・使用量がかかる)。
#
# usage: run-benchmark.sh --skill NAME --out DIR [--model M] [--runs N (既定 2)] [--concurrency N (既定 2)]
#                         [--judge-model M] [--max-cost-usd USD] [--allow-tools LIST]
#   --judge-model : LLM grader のモデル (既定は CLI の既定 = haiku)
#   --allow-tools : CLI に許可する要承認ツール (Bash / Write / Edit 等)。既定は各 case の allowed_tools に書かれたものの和集合。
#                   Bash を許可すると sandbox (Linux は bubblewrap と socat) が必要になる
#   run_errors / first_error : 起動できなかった run (sandbox 不足・認証切れ等) の数と最初のエラー。0 でなければ結果は当てにならない
# 出力: DIR/benchmark.json (CLI の生結果) / DIR/report.html / DIR/benchmark-summary.json (下の形式) / DIR/benchmark.log
#   {"cases", "pass_rate", "pass_rate_without", "score", "score_without", "mean_delta", "cost_usd", "duration_s",
#    "partial", "run_errors", "first_error", "per_case": [{"name", "pass_rate", "pass_rate_without", "delta"}]}
# stdout: "OK benchmark: <name> pass_rate=… without=… delta=…"
set -euo pipefail
# shellcheck source=scripts/skill-eval/lib.sh
source "$(dirname "$0")/lib.sh"

skill="" out="" model="" runs=2 concurrency=2 max_cost="" allow_tools="" judge=""
while [ $# -gt 0 ]; do
  case "$1" in
    --skill) skill="${2:?}"; shift 2 ;;
    --out) out="${2:?}"; shift 2 ;;
    --model) model="${2:?}"; shift 2 ;;
    --runs) runs="${2:?}"; shift 2 ;;
    --concurrency) concurrency="${2:?}"; shift 2 ;;
    --max-cost-usd) max_cost="${2:?}"; shift 2 ;;
    --judge-model) judge="${2:?}"; shift 2 ;;
    --allow-tools) allow_tools="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,15p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
[ -n "$skill" ] && [ -n "$out" ] || die "--skill と --out は必須" "" "例: run-benchmark.sh --skill pr-fix-loop --out .local/skill-eval/pr-fix-loop"
require_cmd claude jq yq
dir="$(skill_dir "$skill")"
[ "$(case_count "$dir")" -gt 0 ] || die "skills/$skill/evals/<case>/ が無い" "Benchmark はテストケースを実行する" "先に eval を作り、check-evals.sh を通す"
mkdir -p "$out"
out="$(cd "$out" && pwd)"

if [ -z "$allow_tools" ]; then
  # case の allowed_tools のうち、CLI が operator の許可を求めるもの (読み取り系と Skill 以外)
  allow_tools="$(for p in "$dir"/evals/*/prompt.md; do yq --front-matter=extract '.allowed_tools // [] | .[]' "$p"; done |
    grep -Ev '^(Read|Glob|Grep|Skill)$' | sort -u | paste -sd, -)" || true
fi
args=(plugin eval "$dir" --trust-plugin --no-publish --scaffold --threshold 0
  --runs "$runs" --concurrency "$concurrency"
  --json "$out/benchmark.json" --report "$out/report.html" --output-dir "$out/cli")
[ -n "$allow_tools" ] && args+=(--allow-tools "$allow_tools")
[ -n "$model" ] && args+=(--model "$model")
[ -n "$judge" ] && args+=(--judge-model "$judge")
[ -n "$max_cost" ] && args+=(--max-cost-usd "$max_cost")

# Claude Code の中から呼ばれた時 (ローカル確認) も入れ子で動くようにする
status=0
env -u CLAUDECODE claude "${args[@]}" >"$out/benchmark.log" 2>&1 || status=$?
partial=false
case "$status" in
  0) ;;
  2) partial=true; warn "$skill: --max-cost-usd に達したので途中までの結果で要約する" ;;
  *) tail -n 20 "$out/benchmark.log" >&2
     die "claude plugin eval が失敗した (exit $status)" "case の形式不正・認証切れ・CLI の不具合のどれか" "$out/benchmark.log を見る。形式なら check-evals.sh、認証なら CLAUDE_CODE_OAUTH_TOKEN を確認する" ;;
esac
[ -s "$out/benchmark.json" ] || die "$out/benchmark.json が出力されていない" "" "$out/benchmark.log を見る"

jq --argjson partial "$partial" '
  def avg(f): (map(f) | if length == 0 then null else (add / length) end);
  {
    cases: (.cases | length),
    pass_rate: .aggregates.overallPassRate,
    pass_rate_without: (.cases | avg(.aggregates.passRateWithout)),
    score: .aggregates.overallScore,
    score_without: (.cases | avg(.aggregates.scoreWithout)),
    mean_delta: .aggregates.meanDelta,
    cost_usd: .costUsd,
    duration_s: .durationSeconds,
    partial: $partial,
    run_errors: ([.cases[].arms[][] | select(.error != null)] | length),
    first_error: ([.cases[].arms[][] | .error // empty] | first // null | if . == null then null else .[0:300] end),
    per_case: [.cases[] | {name, pass_rate: .aggregates.passRate, pass_rate_without: .aggregates.passRateWithout, delta: .aggregates.delta}]
  }' "$out/benchmark.json" >"$out/benchmark-summary.json"

errors="$(jq -r .run_errors "$out/benchmark-summary.json")"
if [ "$errors" != 0 ]; then
  total="$(jq '[.cases[].arms[][]] | length' "$out/benchmark.json")"
  [ "$errors" -lt "$total" ] ||
    die "$skill: 全 $total run が起動できなかった" "$(jq -r .first_error "$out/benchmark-summary.json")" "sandbox (bubblewrap / socat) や認証 (CLAUDE_CODE_OAUTH_TOKEN) を確認する"
  warn "$skill: $errors / $total run が起動できなかった: $(jq -r .first_error "$out/benchmark-summary.json")"
fi
jq -r --arg s "$skill" '"OK benchmark: \($s) pass_rate=\(.pass_rate) without=\(.pass_rate_without) delta=\(.mean_delta) cost_usd=\(.cost_usd)"' "$out/benchmark-summary.json"
