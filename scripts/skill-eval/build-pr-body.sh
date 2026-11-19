#!/usr/bin/env bash
# skill-eval の PR 本文 (Markdown) を、各 skill の結果 (*-summary.json) と history から組み立てる。
#
# usage: build-pr-body.sh --results DIR --selected FILE [--model M] [--skill-creator-ref REF] [--notes FILE] [--history-dir DIR]
#   --results  : skill ごとのサブディレクトリ (DIR/<name>/) に run-benchmark.sh / run-improve.sh の出力がある所
#   --selected : select-skills.sh の出力 (評価前の状態。eval を新しく作ったかの判定に使う)
#   --notes    : AI が書いた補足 (Markdown)。あれば「AI のメモ」として末尾に付ける
# stdout: PR 本文
set -euo pipefail
# shellcheck source=scripts/skill-eval/lib.sh
source "$(dirname "$0")/lib.sh"

results="" selected="" model="" ref="" notes="" history_dir="$SE_DEFAULT_HISTORY_DIR"
while [ $# -gt 0 ]; do
  case "$1" in
    --results) results="${2:?}"; shift 2 ;;
    --selected) selected="${2:?}"; shift 2 ;;
    --model) model="${2:?}"; shift 2 ;;
    --skill-creator-ref) ref="${2:?}"; shift 2 ;;
    --notes) notes="${2:?}"; shift 2 ;;
    --history-dir) history_dir="${2:?}"; shift 2 ;;
    -h | --help) sed -n '2,8p' "$0"; exit 0 ;;
    *) die "未知の引数 '$1'" "" "--help で usage を確認する" ;;
  esac
done
[ -n "$results" ] && [ -n "$selected" ] || die "--results と --selected は必須" "" "--help で usage を確認する"
[ -f "$selected" ] || die "$selected が無い" "select-skills.sh の出力が必要" "パスを確認する"
require_cmd jq yq

pct='def pct: if . == null then "-" else ((. * 100 | round | tostring) + "%") end;'

echo "## skill-eval $(date -u +%F)"
echo
echo "skill の Benchmark (\`claude plugin eval\`: skill あり / なし) と Improve (skill-creator の \`run_loop.py\`: description の発火最適化) を回した結果。"
echo "model: \`${model:-default}\` / skill-creator: \`${ref:-?}\`"
echo
echo "| skill | Benchmark あり / なし (Δ) | 前回のあり | 発火テスト 前 → 後 | description | 備考 |"
echo "|---|---|---|---|---|---|"

new_evals=() changed=() errors=()
for name in $(jq -r '.[].name' "$selected"); do
  file="$history_dir/$name.jsonl"
  last='null' prev='null'
  if [ -s "$file" ]; then
    last="$(tail -n 1 "$file")"
    [ "$(wc -l <"$file")" -ge 2 ] && prev="$(tail -n 2 "$file" | head -n 1)"
  fi
  before_cases="$(jq -r --arg n "$name" '.[] | select(.name == $n) | .cases' "$selected")"
  before_trigger="$(jq -r --arg n "$name" '.[] | select(.name == $n) | .has_trigger_set' "$selected")"
  note=""
  if [ "$before_cases" = 0 ] || [ "$before_trigger" = false ]; then
    new_evals+=("$name")
    note="eval を新規作成"
  fi
  err="$(jq -r '.error // empty' <<<"$last")"
  if [ -n "$err" ]; then
    errors+=("$name: $err")
    note="${note:+$note / }失敗 (下記)"
  fi
  run_errors="$(jq -r '.benchmark.run_errors // 0' <<<"$last")"
  [ "$run_errors" = 0 ] || note="${note:+$note / }起動失敗 run $run_errors 件"
  [ "$(jq -r '.improve.applied // false' <<<"$last")" = true ] && changed+=("$name")
  jq -r --arg name "$name" --arg note "$note" --argjson prev "$prev" "$pct"'
    def bench: if .benchmark == null then "-" else
      "\(.benchmark.pass_rate | pct) / \(.benchmark.pass_rate_without | pct) (\(.benchmark.mean_delta // 0 | . * 100 | round | if . > 0 then "+\(.)" else tostring end)pt)\(if .benchmark.partial then " ※途中まで" else "" end)" end;
    def trig: if .improve == null then "-" else "\(.improve.before.test // .improve.before.train) → \(.improve.after.test // .improve.after.train)" end;
    "| \($name) | \(bench) | \(if $prev == null or $prev.benchmark == null then "-" else ($prev.benchmark.pass_rate | pct) end) | \(trig) | \(if .improve.applied // false then "**更新**" elif .improve == null then "-" else "据え置き" end) | \($note) |"
  ' <<<"$last"
done
echo
echo "- Benchmark: テストケースの pass rate。Δ は skill あり − なし。Δ が 0 付近なら、テストが易しすぎるか skill が効いていない"
echo "- 発火テスト: held-out test セットで description が正しく発火 / 非発火した数。test で真に良くなった時だけ description を書き換える"

if [ "${#changed[@]}" -gt 0 ]; then
  echo
  echo "### description の変更"
  for name in "${changed[@]}"; do
    s="$results/$name/improve-summary.json"
    [ -f "$s" ] || continue
    echo
    echo "<details><summary>$name ($(jq -r '"\(.before.test) → \(.after.test)"' "$s"))</summary>"
    echo
    echo "**前**"
    echo
    jq -r '.original_description' "$s" | sed 's/^/> /'
    echo
    echo "**後**"
    echo
    jq -r '.best_description' "$s" | sed 's/^/> /'
    echo
    echo "</details>"
  done
fi

if [ "${#new_evals[@]}" -gt 0 ]; then
  echo
  echo "### eval を新しく作った skill"
  echo
  echo "AI が作ったテストケース (\`skills/<name>/evals/<case>/\`) と発火テスト (\`skills/<name>/evals/trigger-eval.json\`)。次回以降の基準になるので、意図に沿っているか見てほしい。"
  for name in "${new_evals[@]}"; do
    echo "- $name: $(find "$SE_REPO_ROOT/skills/$name/evals" -mindepth 2 -maxdepth 2 -name prompt.md 2>/dev/null | wc -l | tr -d ' ') cases"
  done
fi

if [ "${#errors[@]}" -gt 0 ]; then
  echo
  echo "### 失敗"
  printf -- '- %s\n' "${errors[@]}"
  echo
  echo "詳細は workflow run の artifact \`skill-eval-reports\` (各 skill の *.log / report.html)。"
fi

if [ -n "$notes" ] && [ -s "$notes" ]; then
  echo
  echo "### AI のメモ"
  echo
  cat "$notes"
fi

echo
echo "### レビューしてほしい点"
echo "- description の変更が skill の意図を外れていないか (発火テストに過剰適合していないか)。README の説明列・詳細ドキュメントとずれたら直す"
echo "- 新しい eval の grader が客観的に判定できる内容か。skill なしでも通る易しすぎる case は削るか難しくする"
echo "- 各 skill の HTML レポートは workflow run の artifact \`skill-eval-reports\` にある"
