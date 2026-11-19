#!/usr/bin/env bash
# 今回評価する skill を選ぶ。Archived 以外の skills/*/SKILL.md から、最後に評価した日 (history) が古い順に --max 件。
# 一度も評価していない skill が最優先。同じ日付なら名前順。
#
# usage: select-skills.sh [--max N (既定 3)] [--only a,b] [--history-dir DIR]
#   --only : 対象を名前で指定する (順序・件数制限は無視。Archived でも指定すれば含める)
# stdout: JSON 配列 [{"name","status","last_run","cases","has_trigger_set"}]
set -euo pipefail
# shellcheck source=scripts/skill-eval/lib.sh
source "$(dirname "$0")/lib.sh"

max=3 only="" history_dir="$SE_DEFAULT_HISTORY_DIR"
while [ $# -gt 0 ]; do
  case "$1" in
    --max) max="${2:?}"; shift 2 ;;
    --only) only="${2:?}"; shift 2 ;;
    --history-dir) history_dir="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,8p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
[[ "$max" =~ ^[0-9]+$ ]] || die "--max '$max' が数値でない" "" "例: --max 3"
require_cmd yq jq

names=()
if [ -n "$only" ]; then
  IFS=',' read -r -a names <<<"$only"
else
  for f in "$SE_REPO_ROOT"/skills/*/SKILL.md; do
    names+=("$(basename "$(dirname "$f")")")
  done
fi

rows=""
for name in "${names[@]}"; do
  name="$(echo "$name" | tr -d ' ')"
  [ -n "$name" ] || continue
  dir="$(skill_dir "$name")"
  status="$(skill_meta "$dir" .metadata.status)"
  if [ -z "$only" ] && [ "$status" = "Archived" ]; then
    continue
  fi
  last_run=""
  if [ -s "$history_dir/$name.jsonl" ]; then
    last_run="$(tail -n 1 "$history_dir/$name.jsonl" | jq -r '.date // ""')"
  fi
  trigger=false
  [ -f "$dir/evals/trigger-eval.json" ] && trigger=true
  rows+="$(jq -cn --arg name "$name" --arg status "$status" --arg last_run "$last_run" \
    --argjson cases "$(case_count "$dir")" --argjson trigger "$trigger" \
    '{name: $name, status: $status, last_run: $last_run, cases: $cases, has_trigger_set: $trigger}')"$'\n'
done

if [ -n "$only" ]; then
  printf '%s' "$rows" | jq -s '.'
else
  printf '%s' "$rows" | jq -s --argjson max "$max" 'sort_by(.last_run, .name) | .[:$max]'
fi
