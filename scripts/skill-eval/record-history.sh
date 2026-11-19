#!/usr/bin/env bash
# 1 skill 分の評価結果を .github/skill-eval/history/<name>.jsonl に 1 行追記する (Benchmark の推移を追う SSoT)。
# 失敗した skill も --error 付きで記録する (次回の選定で後回しになり、同じ skill で詰まり続けない)。
#
# usage: record-history.sh --skill NAME --out DIR --model M [--skill-creator-ref REF] [--error MSG] [--date YYYY-MM-DD] [--history-dir DIR]
#   --out : run-benchmark.sh / run-improve.sh に渡したのと同じディレクトリ (*-summary.json を読む。無ければ null)
# stdout: "OK history: <file>"
set -euo pipefail
# shellcheck source=scripts/skill-eval/lib.sh
source "$(dirname "$0")/lib.sh"

skill="" out="" model="" ref="" error="" day="" history_dir="$SE_DEFAULT_HISTORY_DIR"
while [ $# -gt 0 ]; do
  case "$1" in
    --skill) skill="${2:?}"; shift 2 ;;
    --out) out="${2:?}"; shift 2 ;;
    --model) model="${2:?}"; shift 2 ;;
    --skill-creator-ref) ref="${2:?}"; shift 2 ;;
    --error) error="${2:?}"; shift 2 ;;
    --date) day="${2:?}"; shift 2 ;;
    --history-dir) history_dir="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,7p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
[ -n "$skill" ] && [ -n "$out" ] && [ -n "$model" ] || die "--skill / --out / --model は必須" "" "--help で usage を確認する"
require_cmd jq yq
dir="$(skill_dir "$skill")"
[ -n "$day" ] || day="$(date -u +%F)"

read_or_null() { if [ -s "$1" ]; then cat "$1"; else echo null; fi; }
benchmark="$(read_or_null "$out/benchmark-summary.json")"
improve="$(read_or_null "$out/improve-summary.json")"

mkdir -p "$history_dir"
file="$history_dir/$skill.jsonl"
jq -cn --arg date "$day" --arg model "$model" --arg ref "$ref" --arg error "$error" \
  --arg status "$(skill_meta "$dir" .metadata.status)" --argjson cases "$(case_count "$dir")" \
  --argjson benchmark "$benchmark" --argjson improve "$improve" '
  {
    date: $date, model: $model, skill_creator_ref: (if $ref == "" then null else $ref end),
    status: $status, cases: $cases,
    benchmark: (if $benchmark == null then null else ($benchmark | del(.per_case)) end),
    improve: (if $improve == null then null else ($improve | {before, after, improved, applied}) end),
    error: (if $error == "" then null else $error end)
  }' >>"$file"
echo "OK history: ${file#"$SE_REPO_ROOT"/}"
