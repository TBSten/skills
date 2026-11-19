#!/usr/bin/env bash
# skill の eval (テストケースと発火テスト) が skill-eval で使える形式かを検証する。AI が eval を書いた後に実行する。
#   - evals/<case>/ : prompt.md (frontmatter + 本文) / case.yaml (schema_version "1.1"・name = フォルダ名・expected_outcome) /
#                     graders/*.md (frontmatter に type) を持つ。形式の説明は .github/skill-eval/case-format.md
#   - evals/trigger-eval.json : [{"query": string, "should_trigger": bool}] で true / false が各 --min-each 件以上
#
# usage: check-evals.sh --skill NAME [--min-each N (既定 8)]
# stdout: 問題が無ければ "OK check-evals: <name> cases=N trigger=T/F"。問題があれば全件を stderr に出して exit 1
set -euo pipefail
# shellcheck source=scripts/skill-eval/lib.sh
source "$(dirname "$0")/lib.sh"

skill="" min_each=8
while [ $# -gt 0 ]; do
  case "$1" in
    --skill) skill="${2:?}"; shift 2 ;;
    --min-each) min_each="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,9p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
[ -n "$skill" ] || die "--skill は必須" "" "例: check-evals.sh --skill pr-fix-loop"
require_cmd yq jq
dir="$(skill_dir "$skill")"
schemas=".github/skill-eval/case-format.md の形式に直す"

errors=()
err() { errors+=("$1"); }

# frontmatter (先頭の --- から次の --- まで) を YAML として読めるか
frontmatter_ok() {
  [ "$(head -n 1 "$1")" = "---" ] && yq --front-matter=extract '.' "$1" >/dev/null 2>&1
}

cases=0
if [ -d "$dir/evals" ]; then
  for case_dir in "$dir"/evals/*/; do
    case_dir="${case_dir%/}"
    name="$(basename "$case_dir")"
    case "$name" in results | mocks | node_modules | files) continue ;; esac
    rel="skills/$skill/evals/$name"
    if [ ! -f "$case_dir/prompt.md" ]; then
      err "$rel: prompt.md が無い"
      continue
    fi
    cases=$((cases + 1))
    [[ "$name" =~ ^[a-z][a-z0-9-]*$ ]] && ! [[ "$name" =~ ^(true|false|null)$ ]] ||
      err "$rel: フォルダ名は英小文字・数字・ハイフンで、英字で始める (true/false/null 不可)"
    if frontmatter_ok "$case_dir/prompt.md"; then
      body="$(awk 'BEGIN{n=0} /^---[[:space:]]*$/ && n<2 {n++; next} n>=2' "$case_dir/prompt.md" | tr -d '[:space:]')"
      [ -n "$body" ] || err "$rel/prompt.md: frontmatter の後にプロンプト本文が無い"
      bad_keys="$(yq --front-matter=extract 'keys | .[] | select(. != "tags" and . != "max_turns" and . != "timeout_seconds" and . != "allowed_tools")' "$case_dir/prompt.md")"
      [ -z "$bad_keys" ] || err "$rel/prompt.md: frontmatter に未知のキー ($(echo "$bad_keys" | tr '\n' ' ')) がある (CLI が case を読めなくなる)"
    else
      err "$rel/prompt.md: 先頭が --- の frontmatter になっていない、または YAML として読めない"
    fi
    if [ -f "$case_dir/case.yaml" ]; then
      [ "$(yq '.schema_version | tag' "$case_dir/case.yaml")" = "!!str" ] && [ "$(yq '.schema_version' "$case_dir/case.yaml")" = "1.1" ] ||
        err "$rel/case.yaml: schema_version は文字列 \"1.1\" (引用符付き) にする"
      [ "$(yq '.name // ""' "$case_dir/case.yaml")" = "$name" ] || err "$rel/case.yaml: name をフォルダ名 '$name' と同じにする"
      [ -n "$(yq '.expected_outcome // ""' "$case_dir/case.yaml" | tr -d '[:space:]')" ] || err "$rel/case.yaml: expected_outcome が空"
    else
      err "$rel: case.yaml が無い"
    fi
    graders=0
    for g in "$case_dir"/graders/*.md; do
      [ -f "$g" ] || continue
      graders=$((graders + 1))
      if ! frontmatter_ok "$g" || [ -z "$(yq --front-matter=extract '.type // ""' "$g")" ]; then
        err "$rel/graders/$(basename "$g"): frontmatter に type が無い (CLI はこの grader を無視する)"
      fi
    done
    [ "$graders" -gt 0 ] || err "$rel: graders/*.md が 1 つも無い (grader の無い case は読み込みに失敗する)"
  done
fi
[ "$cases" -gt 0 ] || err "skills/$skill/evals/<case>/prompt.md が 1 つも無い"

trigger="$dir/evals/trigger-eval.json"
t_true=0 t_false=0
if [ ! -f "$trigger" ]; then
  err "skills/$skill/evals/trigger-eval.json が無い"
elif ! jq -e 'type == "array" and all(.[]; (.query | type == "string") and (.query | length > 0) and (.should_trigger | type == "boolean"))' "$trigger" >/dev/null 2>&1; then
  err "skills/$skill/evals/trigger-eval.json: [{\"query\": \"...\", \"should_trigger\": true|false}] の配列になっていない"
else
  t_true="$(jq '[.[] | select(.should_trigger)] | length' "$trigger")"
  t_false="$(jq '[.[] | select(.should_trigger | not)] | length' "$trigger")"
  [ "$t_true" -ge "$min_each" ] && [ "$t_false" -ge "$min_each" ] ||
    err "skills/$skill/evals/trigger-eval.json: should_trigger true / false を各 $min_each 件以上にする (今: $t_true / $t_false)"
  [ "$(jq '[.[].query] | length == (unique | length)' "$trigger")" = true ] ||
    err "skills/$skill/evals/trigger-eval.json: 同じ query が重複している"
fi

if [ "${#errors[@]}" -gt 0 ]; then
  {
    echo "ERROR: skills/$skill の eval に ${#errors[@]} 件の問題がある"
    printf '  - %s\n' "${errors[@]}"
    echo "  fix: $schemas"
  } >&2
  exit 1
fi
echo "OK check-evals: $skill cases=$cases trigger=$t_true/$t_false"
